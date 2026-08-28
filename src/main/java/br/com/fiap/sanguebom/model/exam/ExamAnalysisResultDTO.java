package br.com.fiap.sanguebom.model.exam;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExamAnalysisResultDTO(
        Long riskAssessmentId,
        BigDecimal score,
        RiskAssessmentLevel level,
        String explanation,
        OffsetDateTime createdAt,
        Long userId,
        Long examId,
        Long healthUnitId
) {
}
