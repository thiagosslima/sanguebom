package br.com.fiap.sanguebom.rulesMotor;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;

import java.math.BigDecimal;

public class RiskAssessmentLevelResolver {

    private static final BigDecimal MODERATE_MIN_SCORE =
            new BigDecimal("3.00");

    private static final BigDecimal HIGH_MIN_SCORE =
            new BigDecimal("6.00");

    private static final BigDecimal VERY_HIGH_MIN_SCORE =
            new BigDecimal("8.00");

    private RiskAssessmentLevelResolver() {
    }

    public static RiskAssessmentLevel resolve(BigDecimal score) {

        if (score.compareTo(VERY_HIGH_MIN_SCORE) >= 0) {
            return RiskAssessmentLevel.VERY_HIGH;
        }

        if (score.compareTo(HIGH_MIN_SCORE) >= 0) {
            return RiskAssessmentLevel.HIGH;
        }

        if (score.compareTo(MODERATE_MIN_SCORE) >= 0) {
            return RiskAssessmentLevel.MODERATE;
        }

        return RiskAssessmentLevel.LOW;
    }
}
