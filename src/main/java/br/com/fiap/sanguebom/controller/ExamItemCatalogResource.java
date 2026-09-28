package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.config.swagger.docs.ExamItemCatalogResourceDocs;
import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.ExamItemService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping(value = "/api/v1/exam-items", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamItemCatalogResource implements ExamItemCatalogResourceDocs {

    private final ExamItemService examItemService;

    public ExamItemCatalogResource(final ExamItemService examItemService) {
        this.examItemService = examItemService;
    }

    @GetMapping
    @Override
    public ResponseEntity<PageResponse<ExamItemCatalogDTO>> getExamItems(
            @RequestParam(name = "category", required = false) final String category,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) final int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) final int size) {
        return ResponseEntity.ok(examItemService.findActiveCatalogItems(category, page, size));
    }

    @GetMapping("/{code}/reference-ranges")
    @Override
    public ResponseEntity<List<ReferenceRangeCatalogDTO>> getReferenceRanges(
            @PathVariable(name = "code") final String code) {
        return ResponseEntity.ok(examItemService.findCurrentReferenceRanges(code));
    }
}
