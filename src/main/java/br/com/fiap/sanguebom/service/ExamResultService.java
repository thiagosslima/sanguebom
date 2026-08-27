package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.dtos.ExamResultDTO;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final ExamItemRepository examItemRepository;

    public List<ExamResultDTO> findAll() {
        final List<ExamResult> examResults = examResultRepository.findAll(Sort.by("id"));
        return examResults.stream()
                .map(examResult -> mapToDTO(examResult, new ExamResultDTO()))
                .toList();
    }

    public ExamResultDTO get(final Long id) {
        return examResultRepository.findById(id)
                .map(examResult -> mapToDTO(examResult, new ExamResultDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ExamResultDTO examResultDTO) {
        final ExamResult examResult = new ExamResult();
        mapToEntity(examResultDTO, examResult);
        return examResultRepository.save(examResult).getId();
    }

    public void update(final Long id, final ExamResultDTO examResultDTO) {
        final ExamResult examResult = examResultRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(examResultDTO, examResult);
        examResultRepository.save(examResult);
    }

    private ExamResultDTO mapToDTO(final ExamResult examResult, final ExamResultDTO examResultDTO) {
        examResultDTO.setId(examResult.getId());
        examResultDTO.setValueNumeric(examResult.getValueNumeric());
        examResultDTO.setValueText(examResult.getValueText());
        examResultDTO.setUnit(examResult.getUnit());
        examResultDTO.setFlag(examResult.getFlag());
        examResultDTO.setCreatedAt(examResult.getCreatedAt());
        examResultDTO.setExam(examResult.getExam() == null ? null : examResult.getExam().getId());
        examResultDTO.setExamItem(examResult.getExamItem() == null ? null : examResult.getExamItem().getId());
        return examResultDTO;
    }

    private ExamResult mapToEntity(final ExamResultDTO examResultDTO, final ExamResult examResult) {
        examResult.setValueNumeric(examResultDTO.getValueNumeric());
        examResult.setValueText(examResultDTO.getValueText());
        examResult.setUnit(examResultDTO.getUnit());
        examResult.setFlag(examResultDTO.getFlag());
        examResult.setCreatedAt(examResultDTO.getCreatedAt());
        final Exam exam = examResultDTO.getExam() == null ? null : examRepository.findById(examResultDTO.getExam())
                .orElseThrow(() -> new NotFoundException("exam not found"));
        examResult.setExam(exam);
        final ExamItem examItem = examResultDTO.getExamItem() == null ? null : examItemRepository.findById(examResultDTO.getExamItem())
                .orElseThrow(() -> new NotFoundException("examItem not found"));
        examResult.setExamItem(examItem);
        return examResult;
    }
}
