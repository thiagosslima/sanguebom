package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.dtos.ExamDTO;
import br.com.fiap.sanguebom.service.ExamService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/exams", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamResource {

    private final ExamService examService;

    public ExamResource(final ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ResponseEntity<List<ExamDTO>> getAllExams() {
        return ResponseEntity.ok(examService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamDTO> getExam(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(examService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createExam(@RequestBody @Valid final ExamDTO examDTO) {
        final Long createdId = examService.create(examDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateExam(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ExamDTO examDTO) {
        examService.update(id, examDTO);
        return ResponseEntity.ok(id);
    }
}
