package br.com.fiap.sanguebom.rulesMotor.exam;

import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import br.com.fiap.sanguebom.model.riskAssessment.RiskClassification;
import br.com.fiap.sanguebom.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentClassifierTest {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    @Mock
    private MessageService messageService;

    @InjectMocks
    private RiskAssessmentClassifier classifier;

    @Test
    @DisplayName("Classifica o score e busca a explicação da mensagem correspondente ao nível")
    void classifiesAndResolvesExplanation() {
        when(messageService.getMessage(ApplicationMessage.RISK_ASSESSMENT_HIGH_EXPLANATION, PT_BR))
                .thenReturn("Risco alto");

        RiskClassification result = classifier.classify(new BigDecimal("6.50"), PT_BR);

        assertThat(result.level()).isEqualTo(RiskAssessmentLevel.HIGH);
        assertThat(result.explanation()).isEqualTo("Risco alto");
    }

    @Test
    @DisplayName("A explicação é buscada no locale recebido, e não no locale padrão da JVM")
    void usesGivenLocale() {
        Locale en = Locale.ENGLISH;
        when(messageService.getMessage(ApplicationMessage.RISK_ASSESSMENT_LOW_EXPLANATION, en))
                .thenReturn("Low risk");

        RiskClassification result = classifier.classify(BigDecimal.ZERO, en);

        assertThat(result.explanation()).isEqualTo("Low risk");
        verify(messageService).getMessage(ApplicationMessage.RISK_ASSESSMENT_LOW_EXPLANATION, en);
    }

    @Test
    @DisplayName("Cada faixa de score leva à mensagem do seu próprio nível")
    void mapsEachLevelToItsOwnMessage() {
        when(messageService.getMessage(ApplicationMessage.RISK_ASSESSMENT_VERY_HIGH_EXPLANATION, PT_BR))
                .thenReturn("Risco muito alto");
        when(messageService.getMessage(ApplicationMessage.RISK_ASSESSMENT_MODERATE_EXPLANATION, PT_BR))
                .thenReturn("Risco moderado");

        assertThat(classifier.classify(new BigDecimal("9"), PT_BR).level())
                .isEqualTo(RiskAssessmentLevel.VERY_HIGH);
        assertThat(classifier.classify(new BigDecimal("4"), PT_BR).level())
                .isEqualTo(RiskAssessmentLevel.MODERATE);
    }
}
