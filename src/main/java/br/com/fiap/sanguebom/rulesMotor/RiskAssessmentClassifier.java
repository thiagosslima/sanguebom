package br.com.fiap.sanguebom.rulesMotor;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import br.com.fiap.sanguebom.model.riskAssessment.RiskClassification;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class RiskAssessmentClassifier {

    private final MessageSource messageSource;

    public RiskAssessmentClassifier(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public RiskClassification classify(
            BigDecimal score,
            Locale locale
    ) {

        RiskAssessmentLevel level = RiskAssessmentLevelResolver.resolve(score);

        String explanation = messageSource.getMessage(
                getMessageKey(level),
                null,
                locale
        );

        return new RiskClassification(
                level,
                explanation
        );
    }

    private String getMessageKey(RiskAssessmentLevel level) {
        return switch (level) {
            case LOW -> "risk-assessment.level.low.explanation";
            case MODERATE -> "risk-assessment.level.moderate.explanation";
            case HIGH -> "risk-assessment.level.high.explanation";
            case VERY_HIGH -> "risk-assessment.level.very-high.explanation";
        };
    }
}
