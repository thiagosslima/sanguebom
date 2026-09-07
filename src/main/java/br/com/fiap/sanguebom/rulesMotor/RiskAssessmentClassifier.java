package br.com.fiap.sanguebom.rulesMotor;

import br.com.fiap.sanguebom.service.MessageService;
import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import br.com.fiap.sanguebom.model.riskAssessment.RiskClassification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class RiskAssessmentClassifier {

    private final MessageService messageService;

    public RiskAssessmentClassifier(MessageService messageService) {
        this.messageService = messageService;
    }

    public RiskClassification classify(
            BigDecimal score,
            Locale locale
    ) {

        RiskAssessmentLevel level = RiskAssessmentLevelResolver.resolve(score);

        String explanation = messageService.getMessage(
                getMessageKey(level),
                locale
        );

        return new RiskClassification(
                level,
                explanation
        );
    }

    private String getMessageKey(RiskAssessmentLevel level) {
        return switch (level) {
            case LOW, NORMAL -> "risk-assessment.level.low.explanation";
            case MODERATE, ALERTA -> "risk-assessment.level.moderate.explanation";
            case HIGH -> "risk-assessment.level.high.explanation";
            case VERY_HIGH -> "risk-assessment.level.very-high.explanation";
        };
    }
}
