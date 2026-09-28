package br.com.fiap.sanguebom.model.doctor;

import br.com.fiap.sanguebom.model.enums.ExamResultFlag;

import java.math.BigDecimal;

public record ReferenceRuleSnapshotDTO(
        Long id,
        BigDecimal minValue,
        BigDecimal maxValue,
        Boolean minInclusive,
        Boolean maxInclusive,
        ExamResultFlag level,
        String description,
        String version
) {
}
