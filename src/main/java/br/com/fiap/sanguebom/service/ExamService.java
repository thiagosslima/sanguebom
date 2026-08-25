package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.*;
import br.com.fiap.sanguebom.events.BeforeDeleteAppUser;
import br.com.fiap.sanguebom.events.BeforeDeleteExam;
import br.com.fiap.sanguebom.events.BeforeDeleteHealthUnit;
import br.com.fiap.sanguebom.mapper.ExamMapper;
import br.com.fiap.sanguebom.model.ExamRecoverDTO;
import br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
import br.com.fiap.sanguebom.model.exam.ExamStatus;
import br.com.fiap.sanguebom.repos.AppUserRepository;
import br.com.fiap.sanguebom.repos.ExamItemRepository;
import br.com.fiap.sanguebom.repos.ExamRepository;
import br.com.fiap.sanguebom.repos.HealthUnitRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import br.com.fiap.sanguebom.util.ReferencedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final AppUserRepository appUserRepository;
    private final HealthUnitRepository healthUnitRepository;
    private final ApplicationEventPublisher publisher;
    private final UserServiceHelper userServiceHelper;
    private final ExamItemRepository examItemRepository;

    private final ExamMapper examMapper;

    public ExamService(final ExamRepository examRepository,
            final AppUserRepository appUserRepository,
            final HealthUnitRepository healthUnitRepository,
            final ApplicationEventPublisher publisher,
            final UserServiceHelper userServiceHelper,
            final ExamItemRepository examItemRepository,
            final ExamMapper examMapper
    ) {
        this.examRepository = examRepository;
        this.appUserRepository = appUserRepository;
        this.healthUnitRepository = healthUnitRepository;
        this.publisher = publisher;
        this.userServiceHelper = userServiceHelper;
        this.examItemRepository = examItemRepository;
        this.examMapper = examMapper;
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

    public Long create(final ExamCreateDTO examDTO) {

        AppUser user = userServiceHelper.getUserByIdOrFail(examDTO.userId());

        Long healthUnitId = examDTO.healthUnitId();

        HealthUnit healthUnit = healthUnitRepository.findById(healthUnitId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Unidade de saúde não encontrada para o id: %d", healthUnitId)));

        examDTO.analyzedItems().forEach(examResultDTO -> {
            ExamItem examItem = examItemRepository.findActiveById(examResultDTO.examItemId())
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Item de exame não encontrado para o id: %d", examResultDTO.examItemId())));
        });

        Set<Long> examResultsIds = examDTO.analyzedItems().stream()
                .map(ExamResultDTO::examItemId)
                .collect(Collectors.toSet());

        if(examResultsIds.size() != examDTO.analyzedItems().size()){
            throw new IllegalArgumentException("Não pode haver itens de exame duplicados");
        }


        final Exam exam = examMapper.toEntity(examDTO);
        exam.setStatus(ExamStatus.COLLECTED);
        exam.setHealthUnit(healthUnit);
        exam.setUser(user);

        return examRepository.save(exam).getId();
    }

    public void update(final Long id, final ExamRecoverDTO examRecoverDTO) {
        examRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        Exam exam = examMapper.toEntity(examRecoverDTO);
        examRepository.save(exam);
    }

    public void delete(final Long id) {
        final Exam exam = examRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteExam(id));
        examRepository.delete(exam);
    }


    @EventListener(BeforeDeleteAppUser.class)
    public void on(final BeforeDeleteAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final Exam userExam = examRepository.findFirstByUserId(event.getId());
        if (userExam != null) {
            referencedException.setKey("appUser.exam.user.referenced");
            referencedException.addParam(userExam.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteHealthUnit.class)
    public void on(final BeforeDeleteHealthUnit event) {
        final ReferencedException referencedException = new ReferencedException();
        final Exam healthUnitExam = examRepository.findFirstByHealthUnitId(event.getId());
        if (healthUnitExam != null) {
            referencedException.setKey("healthUnit.exam.healthUnit.referenced");
            referencedException.addParam(healthUnitExam.getId());
            throw referencedException;
        }
    }

}
