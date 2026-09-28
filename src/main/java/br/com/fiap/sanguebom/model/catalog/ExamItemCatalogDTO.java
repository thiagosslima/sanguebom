package br.com.fiap.sanguebom.model.catalog;

public record ExamItemCatalogDTO(
        Long id,
        String code,
        String name,
        String unit,
        String category,
        String description
) {
}
