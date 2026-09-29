package br.com.fiap.sanguebom.config.swagger.schema;

import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ExamItemCatalogResponse")
public record ExamItemCatalogResponseSchema(
        List<ExamItemCatalogDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
}
