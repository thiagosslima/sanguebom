package br.com.fiap.sanguebom.model.doctor;

import br.com.fiap.sanguebom.model.enums.ExamResultFlag;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MarkerTimelinePointDTO(
        Long examId,
        OffsetDateTime collectedAt,
        BigDecimal valueNumeric,
        String valueText,
        String unit,
        ExamResultFlag flag,
        ReferenceRangeSnapshotDTO referenceRange,
        ReferenceRuleSnapshotDTO matchedRule
) {
}
