package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.HealthUnit;
import br.com.fiap.sanguebom.model.dtos.ExamDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.HealthUnitRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final AppUserRepository appUserRepository;
    private final HealthUnitRepository healthUnitRepository;

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
}
