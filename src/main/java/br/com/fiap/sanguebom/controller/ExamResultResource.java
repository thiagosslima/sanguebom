package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.config.swagger.docs.ExamResultResourceDocs;
import br.com.fiap.sanguebom.model.dtos.ExamResultDTO;
import br.com.fiap.sanguebom.service.ExamResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/examResults", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamResultResource implements ExamResultResourceDocs {

    private final ExamResultService examResultService;

    public ExamResultResource(final ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    @GetMapping
    @Override
    public ResponseEntity<List<ExamResultDTO>> getAllExamResults() {
        return ResponseEntity.ok(examResultService.findAll());
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ExamResultDTO> getExamResult(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(examResultService.get(id));
    }

    @PostMapping
    @Override
    public ResponseEntity<Long> createExamResult(
            @RequestBody @Valid final ExamResultDTO examResultDTO) {
        final Long createdId = examResultService.create(examResultDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<Long> updateExamResult(@PathVariable(name = "id") final Long id,
                                                 @RequestBody @Valid final ExamResultDTO examResultDTO) {
        examResultService.update(id, examResultDTO);
        return ResponseEntity.ok(id);
    }
}
