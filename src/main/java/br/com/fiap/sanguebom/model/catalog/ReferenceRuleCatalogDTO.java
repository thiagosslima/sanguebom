package br.com.fiap.sanguebom.model.catalog;

import br.com.fiap.sanguebom.model.enums.ExamResultFlag;

import java.math.BigDecimal;

public record ReferenceRuleCatalogDTO(
        Long id,
        BigDecimal minValue,
        BigDecimal maxValue,
        Boolean minInclusive,
        Boolean maxInclusive,
        ExamResultFlag level,
        BigDecimal score,
        String description,
        String version
) {
}
