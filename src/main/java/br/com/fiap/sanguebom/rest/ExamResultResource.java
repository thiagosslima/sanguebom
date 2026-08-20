package br.com.fiap.sanguebom.rest;

import br.com.fiap.sanguebom.model.ExamResultDTO;
import br.com.fiap.sanguebom.service.ExamResultService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/examResults", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamResultResource {

    private final ExamResultService examResultService;

    public ExamResultResource(final ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    @GetMapping
    public ResponseEntity<List<ExamResultDTO>> getAllExamResults() {
        return ResponseEntity.ok(examResultService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamResultDTO> getExamResult(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(examResultService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createExamResult(
            @RequestBody @Valid final ExamResultDTO examResultDTO) {
        final Long createdId = examResultService.create(examResultDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateExamResult(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ExamResultDTO examResultDTO) {
        examResultService.update(id, examResultDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteExamResult(@PathVariable(name = "id") final Long id) {
        examResultService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
