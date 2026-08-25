package br.com.fiap.sanguebom.rest;

import br.com.fiap.sanguebom.model.ExamRecoverDTO;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
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
    public ResponseEntity<List<ExamRecoverDTO>> getAllExams() {
        return ResponseEntity.ok(examService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamRecoverDTO> getExam(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(examService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createExam(@RequestBody @Valid final ExamCreateDTO examDTO) {
        final Long createdId = examService.create(examDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateExam(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ExamRecoverDTO examRecoverDTO) {
        examService.update(id, examRecoverDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteExam(@PathVariable(name = "id") final Long id) {
        examService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
