package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.AppUser;
import br.com.fiap.sanguebom.domain.Exam;
import br.com.fiap.sanguebom.domain.RiskAssessment;
import br.com.fiap.sanguebom.events.BeforeDeleteAppUser;
import br.com.fiap.sanguebom.events.BeforeDeleteExam;
import br.com.fiap.sanguebom.model.RiskAssessmentDTO;
import br.com.fiap.sanguebom.repos.AppUserRepository;
import br.com.fiap.sanguebom.repos.ExamRepository;
import br.com.fiap.sanguebom.repos.RiskAssessmentRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import br.com.fiap.sanguebom.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AppUserRepository appUserRepository;
    private final ExamRepository examRepository;

    public RiskAssessmentService(final RiskAssessmentRepository riskAssessmentRepository,
            final AppUserRepository appUserRepository, final ExamRepository examRepository) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.appUserRepository = appUserRepository;
        this.examRepository = examRepository;
    }

    public List<RiskAssessmentDTO> findAll() {
        final List<RiskAssessment> riskAssessments = riskAssessmentRepository.findAll(Sort.by("id"));
        return riskAssessments.stream()
                .map(riskAssessment -> mapToDTO(riskAssessment, new RiskAssessmentDTO()))
                .toList();
    }

    public RiskAssessmentDTO get(final Long id) {
        return riskAssessmentRepository.findById(id)
                .map(riskAssessment -> mapToDTO(riskAssessment, new RiskAssessmentDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final RiskAssessmentDTO riskAssessmentDTO) {
        final RiskAssessment riskAssessment = new RiskAssessment();
        mapToEntity(riskAssessmentDTO, riskAssessment);
        return riskAssessmentRepository.save(riskAssessment).getId();
    }

    public void update(final Long id, final RiskAssessmentDTO riskAssessmentDTO) {
        final RiskAssessment riskAssessment = riskAssessmentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(riskAssessmentDTO, riskAssessment);
        riskAssessmentRepository.save(riskAssessment);
    }

    public void delete(final Long id) {
        final RiskAssessment riskAssessment = riskAssessmentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        riskAssessmentRepository.delete(riskAssessment);
    }

    private RiskAssessmentDTO mapToDTO(final RiskAssessment riskAssessment,
            final RiskAssessmentDTO riskAssessmentDTO) {
        riskAssessmentDTO.setId(riskAssessment.getId());
        riskAssessmentDTO.setScore(riskAssessment.getScore());
        riskAssessmentDTO.setLevel(riskAssessment.getLevel());
        riskAssessmentDTO.setRulesVersion(riskAssessment.getRulesVersion());
        riskAssessmentDTO.setExplanation(riskAssessment.getExplanation());
        riskAssessmentDTO.setCreatedAt(riskAssessment.getCreatedAt());
        riskAssessmentDTO.setUser(riskAssessment.getUser() == null ? null : riskAssessment.getUser().getId());
        riskAssessmentDTO.setExam(riskAssessment.getExam() == null ? null : riskAssessment.getExam().getId());
        return riskAssessmentDTO;
    }

    private RiskAssessment mapToEntity(final RiskAssessmentDTO riskAssessmentDTO,
            final RiskAssessment riskAssessment) {
        riskAssessment.setScore(riskAssessmentDTO.getScore());
        riskAssessment.setLevel(riskAssessmentDTO.getLevel());
        riskAssessment.setRulesVersion(riskAssessmentDTO.getRulesVersion());
        riskAssessment.setExplanation(riskAssessmentDTO.getExplanation());
        riskAssessment.setCreatedAt(riskAssessmentDTO.getCreatedAt());
        final AppUser user = riskAssessmentDTO.getUser() == null ? null : appUserRepository.findById(riskAssessmentDTO.getUser())
                .orElseThrow(() -> new NotFoundException("user not found"));
        riskAssessment.setUser(user);
        final Exam exam = riskAssessmentDTO.getExam() == null ? null : examRepository.findById(riskAssessmentDTO.getExam())
                .orElseThrow(() -> new NotFoundException("exam not found"));
        riskAssessment.setExam(exam);
        return riskAssessment;
    }

    @EventListener(BeforeDeleteAppUser.class)
    public void on(final BeforeDeleteAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final RiskAssessment userRiskAssessment = riskAssessmentRepository.findFirstByUserId(event.getId());
        if (userRiskAssessment != null) {
            referencedException.setKey("appUser.riskAssessment.user.referenced");
            referencedException.addParam(userRiskAssessment.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteExam.class)
    public void on(final BeforeDeleteExam event) {
        final ReferencedException referencedException = new ReferencedException();
        final RiskAssessment examRiskAssessment = riskAssessmentRepository.findFirstByExamId(event.getId());
        if (examRiskAssessment != null) {
            referencedException.setKey("exam.riskAssessment.exam.referenced");
            referencedException.addParam(examRiskAssessment.getId());
            throw referencedException;
        }
    }

}
