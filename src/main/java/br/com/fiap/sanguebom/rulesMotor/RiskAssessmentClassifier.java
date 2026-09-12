package br.com.fiap.sanguebom.rulesMotor;

import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
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
                ApplicationMessage.from(level),
                locale
        );

        return new RiskClassification(
                level,
                explanation
        );
    }

}
