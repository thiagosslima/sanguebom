package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.dtos.RiskAssessmentDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.RiskAssessmentRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AppUserRepository appUserRepository;
    private final ExamRepository examRepository;

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
}
