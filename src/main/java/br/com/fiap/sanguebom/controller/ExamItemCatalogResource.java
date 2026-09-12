package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.ExamItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@Tag(name = "Catálogo público de exames",
        description = "Itens de exame e faixas de referência vigentes usadas pelo sistema")
@RequestMapping(value = "/api/v1/exam-items", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamItemCatalogResource {

    private final ExamItemService examItemService;

    public ExamItemCatalogResource(final ExamItemService examItemService) {
        this.examItemService = examItemService;
    }

    @GetMapping
    @Operation(summary = "Lista itens de exame ativos",
            description = "Consulta pública paginada dos itens de exame disponíveis no catálogo do sistema.")
    public ResponseEntity<PageResponse<ExamItemCatalogDTO>> getExamItems(
            @RequestParam(name = "category", required = false) final String category,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) final int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) final int size) {
        return ResponseEntity.ok(examItemService.findActiveCatalogItems(category, page, size));
    }

    @GetMapping("/{code}/reference-ranges")
    @Operation(summary = "Lista faixas de referência vigentes por item",
            description = "Consulta pública das faixas de referência que o sistema usa hoje, "
                    + "incluindo a fonte e as regras numéricas associadas.")
    public ResponseEntity<List<ReferenceRangeCatalogDTO>> getReferenceRanges(
            @PathVariable(name = "code") final String code) {
        return ResponseEntity.ok(examItemService.findCurrentReferenceRanges(code));
    }
}
