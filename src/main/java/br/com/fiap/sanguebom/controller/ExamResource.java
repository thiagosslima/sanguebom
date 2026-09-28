package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.config.swagger.docs.ExamResourceDocs;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisResultDTO;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
import br.com.fiap.sanguebom.model.exam.ExamRecoverDTO;
import br.com.fiap.sanguebom.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/exams", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamResource implements ExamResourceDocs {

    private final ExamService examService;

    public ExamResource(final ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    @Override
    public ResponseEntity<List<ExamRecoverDTO>> getAllExams() {
        return ResponseEntity.ok(examService.findAll());
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ExamRecoverDTO> getExam(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(examService.get(id));
    }

    @PostMapping
    @Override
    public ResponseEntity<ExamAnalysisResultDTO> createExam(@RequestBody @Valid final ExamCreateDTO examDTO) {
        final ExamAnalysisResultDTO created = examService.create(examDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<Long> updateExam(@PathVariable(name = "id") final Long id,
                                           @RequestBody @Valid final ExamRecoverDTO examRecoverDTO) {
        examService.update(id, examRecoverDTO);
        return ResponseEntity.ok(id);
    }


}
