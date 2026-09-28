package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.config.swagger.docs.ExamItemResourceDocs;
import br.com.fiap.sanguebom.model.dtos.ExamItemDTO;
import br.com.fiap.sanguebom.service.ExamItemService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/examItems", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamItemResource implements ExamItemResourceDocs {

    private final ExamItemService examItemService;

    public ExamItemResource(final ExamItemService examItemService) {
        this.examItemService = examItemService;
    }

    @GetMapping
    @Override
    public ResponseEntity<List<ExamItemDTO>> getAllExamItems() {
        return ResponseEntity.ok(examItemService.findAll());
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ExamItemDTO> getExamItem(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(examItemService.get(id));
    }

    @PostMapping
    @Override
    public ResponseEntity<Long> createExamItem(@RequestBody @Valid final ExamItemDTO examItemDTO) {
        final Long createdId = examItemService.create(examItemDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<Long> updateExamItem(@PathVariable(name = "id") final Long id,
                                               @RequestBody @Valid final ExamItemDTO examItemDTO) {
        examItemService.update(id, examItemDTO);
        return ResponseEntity.ok(id);
    }
}
