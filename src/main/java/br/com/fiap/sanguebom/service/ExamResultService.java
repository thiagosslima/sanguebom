package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.Exam;
import br.com.fiap.sanguebom.domain.ExamItem;
import br.com.fiap.sanguebom.domain.ExamResult;
import br.com.fiap.sanguebom.events.BeforeDeleteExam;
import br.com.fiap.sanguebom.events.BeforeDeleteExamItem;
import br.com.fiap.sanguebom.model.ExamResultDTO;
import br.com.fiap.sanguebom.repos.ExamItemRepository;
import br.com.fiap.sanguebom.repos.ExamRepository;
import br.com.fiap.sanguebom.repos.ExamResultRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import br.com.fiap.sanguebom.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final ExamItemRepository examItemRepository;

    public ExamResultService(final ExamResultRepository examResultRepository,
            final ExamRepository examRepository, final ExamItemRepository examItemRepository) {
        this.examResultRepository = examResultRepository;
        this.examRepository = examRepository;
        this.examItemRepository = examItemRepository;
    }

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

    public void delete(final Long id) {
        final ExamResult examResult = examResultRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        examResultRepository.delete(examResult);
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

    @EventListener(BeforeDeleteExam.class)
    public void on(final BeforeDeleteExam event) {
        final ReferencedException referencedException = new ReferencedException();
        final ExamResult examExamResult = examResultRepository.findFirstByExamId(event.getId()).orElse(null);
        if (examExamResult != null) {
            referencedException.setKey("exam.examResult.exam.referenced");
            referencedException.addParam(examExamResult.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteExamItem.class)
    public void on(final BeforeDeleteExamItem event) {
        final ReferencedException referencedException = new ReferencedException();
        final ExamResult examItemExamResult = examResultRepository.findFirstByExamItemId(event.getId()).orElse(null);
        if (examItemExamResult != null) {
            referencedException.setKey("examItem.examResult.examItem.referenced");
            referencedException.addParam(examItemExamResult.getId());
            throw referencedException;
        }
    }

}
