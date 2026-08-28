package br.com.fiap.sanguebom.model.riskAssessment;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;

public record RiskClassification(
        RiskAssessmentLevel level,
        String explanation
) {
}
