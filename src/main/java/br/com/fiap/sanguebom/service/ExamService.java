package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.mapper.ExamIResultMapper;
import br.com.fiap.sanguebom.mapper.ExamMapper;
import br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.model.exam.ExamRecoverDTO;
import br.com.fiap.sanguebom.model.entities.*;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.model.riskAssessment.RiskClassification;
import br.com.fiap.sanguebom.repository.*;
import br.com.fiap.sanguebom.rulesMotor.RiskAssessmentClassifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final AppUserRepository appUserRepository;
    private final HealthUnitRepository healthUnitRepository;
    private final ApplicationEventPublisher publisher;
    private final UserServiceHelper userServiceHelper;
    private final ExamItemRepository examItemRepository;
    private final ExamResultRepository examResultRepository;
    private final ReferenceRangeRepository referenceRangeRepository;
    private final RuleRepository ruleRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAssessmentClassifier riskAssessmentClassifier;

    private final ExamMapper examMapper;
    private final ExamIResultMapper examIResultMapper;

    public ExamService(final ExamRepository examRepository,
                       final AppUserRepository appUserRepository,
                       final HealthUnitRepository healthUnitRepository,
                       final ApplicationEventPublisher publisher,
                       final UserServiceHelper userServiceHelper,
                       final ExamItemRepository examItemRepository,
                       final ExamMapper examMapper,
                       final ExamResultRepository examResultRepository,
                       final ReferenceRangeRepository referenceRangeRepository,
                       final RuleRepository ruleRepository,
                       final RiskAssessmentRepository riskAssessmentRepository,
                       final RiskAssessmentClassifier riskAssessmentClassifier,
                       ExamIResultMapper examIResultMapper) {
        this.examRepository = examRepository;
        this.appUserRepository = appUserRepository;
        this.healthUnitRepository = healthUnitRepository;
        this.publisher = publisher;
        this.userServiceHelper = userServiceHelper;
        this.examItemRepository = examItemRepository;
        this.examMapper = examMapper;
        this.examResultRepository = examResultRepository;
        this.examIResultMapper = examIResultMapper;
        this.referenceRangeRepository = referenceRangeRepository;
        this.ruleRepository = ruleRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskAssessmentClassifier = riskAssessmentClassifier;
    }

    public List<ExamRecoverDTO> findAll() {
        final List<Exam> exams = examRepository.findAll(Sort.by("id"));
        return exams.stream()
                .map(exam -> examMapper.toDTO(exam))
                .toList();
    }

    public ExamRecoverDTO get(final Long id) {
        return examRepository.findById(id)
                .map(exam -> examMapper.toDTO(exam))
                .orElseThrow(NotFoundException::new);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(final ExamCreateDTO examDTO) {

        AppUser user = userServiceHelper.getUserByIdOrFail(examDTO.userId());

        Long healthUnitId = examDTO.healthUnitId();

        HealthUnit healthUnit = healthUnitRepository.findById(healthUnitId).orElseThrow(() -> new NotFoundException(String.format("Unidade de saúde não encontrada para o id: %d", healthUnitId)));

        List<ExamResultDTO> examResultList = examDTO.analyzedItems();

        Map<Long, ExamItem> examItemMap = new HashMap<>();

        examResultList.forEach(examResultDTO -> {
            ExamItem examItem = examItemRepository.findActiveById(examResultDTO.examItemId()).orElseThrow(()
                    -> new NotFoundException(String.format("Item de exame não encontrado para o id: %d", examResultDTO.examItemId())));
            examItemMap.put(examResultDTO.examItemId(), examItem);
        });

        throwCaseThereAreDuplicatedExamResult(examResultList);

        final Exam exam = examMapper.toEntity(examDTO);

        exam.setStatus(ExamStatus.COLLECTED);
        exam.setHealthUnit(healthUnit);
        exam.setUser(user);
        Exam savedExam = examRepository.save(exam);

        List<ExamResult> examResults = new ArrayList<>();

        for (ExamResultDTO examResultDto : examResultList) {
            ExamResult examResult = examIResultMapper.toEntity(examResultDto);
            ExamItem examItem = examItemMap.get(examResultDto.examItemId());
            examResult.setExam(savedExam);
            examResult.setExamItem(examItem);
            examResult.setUnit(examItem.getUnit());
            examResult.setFlag(ExamResultFlag.IN_ANALYSIS);
            examResults.add(examResult);
        }


        examResultRepository.saveAll(examResults);

        BigDecimal totalScore = BigDecimal.ZERO;
        int evaluatedItems = 0;

        for(ExamResult examResult : examResults) {

            long userAge = ChronoUnit.YEARS.between(user.getBirthDate(), LocalDate.now());
            Sex userSex = user.getHealthProfile().getSex();

            ReferenceRange referenceRange = referenceRangeRepository.findApplicableRangeForUserByExamItem(
                    examResult.getExamItem().getId(),
                    userSex,
                    userAge
            ).orElseThrow(() -> new NotFoundException(
                    String.format("Faixa de referência não encontrada para o item de exame %d", examResult.getExamItem().getId()))
            );

            List<Rule> rules = ruleRepository.findByReferenceRangeId(referenceRange.getId());

            Rule applicableRule = rules.stream()
                    .filter(rule -> rule.appliesTo(examResult.getValueNumeric()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Nehuma regra enocntrada para o valor informado "
                    ));

            examResult.setFlag(applicableRule.getLevel());

            BigDecimal score = applicableRule.getScore();
            totalScore = totalScore.add(score);
            evaluatedItems++;
        }

        BigDecimal finalScore = totalScore.divide(
                new BigDecimal(evaluatedItems),
                2,
                RoundingMode.HALF_UP);

        RiskClassification result = riskAssessmentClassifier.classify(finalScore, Locale.getDefault());

        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setExam(exam);
        riskAssessment.setUser(user);

        riskAssessment.applyAssessment(finalScore, result);

        riskAssessmentRepository.save(riskAssessment);

        return examRepository.save(exam).getId();

    }



    private static void throwCaseThereAreDuplicatedExamResult(List<ExamResultDTO> examResultList) {
        Set<Long> examResultsIds = examResultList.stream()
                .map(ExamResultDTO::examItemId)
                .collect(Collectors.toSet());

        if(examResultsIds.size() != examResultList.size()){
            throw new IllegalArgumentException("Não pode haver itens de exame duplicados");
        }
    }

    private void checkIfAllExamItemsExist(List<ExamResultDTO> examResultList) {

    }

    public void update(final Long id, final ExamRecoverDTO examRecoverDTO) {
        examRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        Exam exam = examMapper.toEntity(examRecoverDTO);
        examRepository.save(exam);
    }


}
