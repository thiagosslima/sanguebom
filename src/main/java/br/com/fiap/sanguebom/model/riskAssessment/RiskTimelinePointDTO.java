package br.com.fiap.sanguebom.model.riskAssessment;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RiskTimelinePointDTO(
        Long id,
        Long examId,
        BigDecimal score,
        RiskAssessmentLevel level,
        OffsetDateTime timestamp) {
}
