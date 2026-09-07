package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.DuplicatedExamResultException;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.mapper.ExamAnalysisResultMapper;
import br.com.fiap.sanguebom.mapper.ExamIResultMapper;
import br.com.fiap.sanguebom.mapper.ExamMapper;
import br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO;
import br.com.fiap.sanguebom.model.exam.*;
import br.com.fiap.sanguebom.model.entities.*;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.repository.*;
import br.com.fiap.sanguebom.rulesMotor.ExamAnalysisService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final RiskAssessmentService riskAssessmentService;
    private final ExamAnalysisService examAnalysisService;
    private final ExamAnalysisResultMapper examAnalysisResultMapper;

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
                       final RiskAssessmentService riskAssessmentService,
                       final ExamAnalysisService examAnalysisService,
                       ExamIResultMapper examIResultMapper,
                       ExamAnalysisResultMapper examAnalysisResultMapper) {
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
        this.riskAssessmentService = riskAssessmentService;
        this.examAnalysisService = examAnalysisService;
        this.examAnalysisResultMapper = examAnalysisResultMapper;
    }

    public List<ExamRecoverDTO> findAll() {
        final List<Exam> exams = examRepository.findAll(Sort.by("id"));
        return exams.stream()
                .map(examMapper::toDTO)
                .toList();
    }

    public ExamRecoverDTO get(final Long id) {
        return examRepository.findById(id)
                .map(examMapper::toDTO)
                .orElseThrow(NotFoundException::new);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamAnalysisResultDTO create(final ExamCreateDTO examDTO) {

        validateExamResults(examDTO.analyzedItems());

        ExamCreationContext context = loadContext(examDTO);

        Exam exam = createExam(examDTO, context);

        List<ExamResult> examResults = createExamResults(
                exam,
                examDTO.analyzedItems(),
                context.examItems()
        );

        ExamAnalysisScore analysisResult =
                examAnalysisService.analyze(
                        exam,
                        examResults,
                        context.user()
                );

        RiskAssessment riskAssessment =
                riskAssessmentService.createRiskAssessment(
                        exam,
                        context.user(),
                        analysisResult
                );

        RiskAssessment savedRA = riskAssessmentRepository.save(riskAssessment);


        return examAnalysisResultMapper.fromRiskAssessmentToAnalysisResult(savedRA);

    }

    private void validateExamResults(List<ExamResultDTO> examResults) {

        throwCaseThereAreDuplicatedExamResult(examResults);
    }

    private ExamCreationContext loadContext(ExamCreateDTO examDTO) {

        AppUser user =
                userServiceHelper.getUserByIdOrFail(examDTO.userId());

        HealthUnit healthUnit =
                healthUnitRepository.findById(examDTO.healthUnitId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        String.format(
                                                "Unidade de saúde não encontrada para o id: %d",
                                                examDTO.healthUnitId()
                                        )
                                )
                        );

        Map<Long, ExamItem> examItems =
                loadExamItems(examDTO.analyzedItems());

        return new ExamCreationContext(
                user,
                healthUnit,
                examItems
        );
    }

    private Map<Long, ExamItem> loadExamItems(
            List<ExamResultDTO> examResults
    ) {

        return examResults.stream()
                .map(ExamResultDTO::examItemId)
                .distinct()
                .collect(Collectors.toMap(
                        examItemId -> examItemId,
                        this::getActiveExamItemOrFail
                ));
    }

    private ExamItem getActiveExamItemOrFail(Long examItemId) {

        return examItemRepository
                .findActiveById(examItemId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format(
                                        "Item de exame não encontrado para o id: %d",
                                        examItemId
                                )
                        )
                );
    }

    private Exam createExam(
            ExamCreateDTO examDTO,
            ExamCreationContext context
    ) {

        Exam exam = examMapper.toEntity(examDTO);

        exam.setStatus(ExamStatus.COLLECTED);
        exam.setUser(context.user());
        exam.setHealthUnit(context.healthUnit());

        return examRepository.save(exam);
    }

    private List<ExamResult> createExamResults(
            Exam exam,
            List<ExamResultDTO> examResultDTOs,
            Map<Long, ExamItem> examItems
    ) {

        List<ExamResult> examResults =
                examResultDTOs.stream()
                        .map(dto ->
                                createExamResult(
                                        exam,
                                        dto,
                                        examItems.get(dto.examItemId())
                                )
                        )
                        .toList();

        return examResultRepository.saveAll(examResults);
    }

    private ExamResult createExamResult(
            Exam exam,
            ExamResultDTO dto,
            ExamItem examItem
    ) {

        ExamResult examResult =
                examIResultMapper.toEntity(dto);

        examResult.setExam(exam);
        examResult.setExamItem(examItem);
        examResult.setUnit(examItem.getUnit());
        examResult.setFlag(ExamResultFlag.IN_ANALYSIS);

        return examResult;
    }



    private void throwCaseThereAreDuplicatedExamResult(List<ExamResultDTO> examResultList) {
        Set<Long> examResultsIds = examResultList.stream()
                .map(ExamResultDTO::examItemId)
                .collect(Collectors.toSet());

        if(examResultsIds.size() != examResultList.size()){
            throw new DuplicatedExamResultException("Não pode haver itens de exame duplicados");
        }
    }


    public void update(final Long id, final ExamRecoverDTO examRecoverDTO) {
        examRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        Exam exam = examMapper.toEntity(examRecoverDTO);
        examRepository.save(exam);
    }


}
