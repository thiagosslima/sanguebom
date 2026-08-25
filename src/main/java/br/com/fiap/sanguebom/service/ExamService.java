package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.AppUser;
import br.com.fiap.sanguebom.domain.Exam;
import br.com.fiap.sanguebom.domain.HealthUnit;
import br.com.fiap.sanguebom.events.BeforeDeleteAppUser;
import br.com.fiap.sanguebom.events.BeforeDeleteExam;
import br.com.fiap.sanguebom.events.BeforeDeleteHealthUnit;
import br.com.fiap.sanguebom.model.ExamDTO;
import br.com.fiap.sanguebom.repos.AppUserRepository;
import br.com.fiap.sanguebom.repos.ExamRepository;
import br.com.fiap.sanguebom.repos.HealthUnitRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import br.com.fiap.sanguebom.util.ReferencedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final AppUserRepository appUserRepository;
    private final HealthUnitRepository healthUnitRepository;
    private final ApplicationEventPublisher publisher;

    public ExamService(final ExamRepository examRepository,
            final AppUserRepository appUserRepository,
            final HealthUnitRepository healthUnitRepository,
            final ApplicationEventPublisher publisher) {
        this.examRepository = examRepository;
        this.appUserRepository = appUserRepository;
        this.healthUnitRepository = healthUnitRepository;
        this.publisher = publisher;
    }

    public List<ExamDTO> findAll() {
        final List<Exam> exams = examRepository.findAll(Sort.by("id"));
        return exams.stream()
                .map(exam -> mapToDTO(exam, new ExamDTO()))
                .toList();
    }

    public ExamDTO get(final Long id) {
        return examRepository.findById(id)
                .map(exam -> mapToDTO(exam, new ExamDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ExamDTO examDTO) {
        final Exam exam = new Exam();
        mapToEntity(examDTO, exam);
        return examRepository.save(exam).getId();
    }

    public void update(final Long id, final ExamDTO examDTO) {
        final Exam exam = examRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(examDTO, exam);
        examRepository.save(exam);
    }

    public void delete(final Long id) {
        final Exam exam = examRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteExam(id));
        examRepository.delete(exam);
    }

    private ExamDTO mapToDTO(final Exam exam, final ExamDTO examDTO) {
        examDTO.setId(exam.getId());
        examDTO.setCollectedAt(exam.getCollectedAt());
        examDTO.setReleasedAt(exam.getReleasedAt());
        examDTO.setStatus(exam.getStatus());
        examDTO.setExternalReference(exam.getExternalReference());
        examDTO.setCreatedAt(exam.getCreatedAt());
        examDTO.setUser(exam.getUser() == null ? null : exam.getUser().getId());
        examDTO.setHealthUnit(exam.getHealthUnit() == null ? null : exam.getHealthUnit().getId());
        return examDTO;
    }

    private Exam mapToEntity(final ExamDTO examDTO, final Exam exam) {
        exam.setCollectedAt(examDTO.getCollectedAt());
        exam.setReleasedAt(examDTO.getReleasedAt());
        exam.setStatus(examDTO.getStatus());
        exam.setExternalReference(examDTO.getExternalReference());
        exam.setCreatedAt(examDTO.getCreatedAt());
        final AppUser user = examDTO.getUser() == null ? null : appUserRepository.findById(examDTO.getUser())
                .orElseThrow(() -> new NotFoundException("user not found"));
        exam.setUser(user);
        final HealthUnit healthUnit = examDTO.getHealthUnit() == null ? null : healthUnitRepository.findById(examDTO.getHealthUnit())
                .orElseThrow(() -> new NotFoundException("healthUnit not found"));
        exam.setHealthUnit(healthUnit);
        return exam;
    }

    @EventListener(BeforeDeleteAppUser.class)
    public void on(final BeforeDeleteAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final Exam userExam = examRepository.findFirstByUserId(event.getId()).orElse(null);
        if (userExam != null) {
            referencedException.setKey("appUser.exam.user.referenced");
            referencedException.addParam(userExam.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteHealthUnit.class)
    public void on(final BeforeDeleteHealthUnit event) {
        final ReferencedException referencedException = new ReferencedException();
        final Exam healthUnitExam = examRepository.findFirstByHealthUnitId(event.getId()).orElse(null);
        if (healthUnitExam != null) {
            referencedException.setKey("healthUnit.exam.healthUnit.referenced");
            referencedException.addParam(healthUnitExam.getId());
            throw referencedException;
        }
    }

}
