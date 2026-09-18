package br.com.fiap.sanguebom.rulesMotor.exam;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As fronteiras são 3.00 (MODERATE), 6.00 (HIGH) e 8.00 (VERY_HIGH),
 * todas inclusivas. Os casos abaixo cobrem cada limite por baixo e por cima.
 */
class RiskAssessmentLevelResolverTest {

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({
            "0.00,  LOW",
            "2.99,  LOW",
            "3.00,  MODERATE",
            "5.99,  MODERATE",
            "6.00,  HIGH",
            "7.99,  HIGH",
            "8.00,  VERY_HIGH",
            "10.00, VERY_HIGH"
    })
    void shouldResolveLevelByScoreBoundaries(String score, RiskAssessmentLevel expected) {
        assertThat(RiskAssessmentLevelResolver.resolve(new BigDecimal(score))).isEqualTo(expected);
    }

    @Test
    @DisplayName("A escala do BigDecimal não muda a classificação")
    void shouldCompareNumericallyRegardlessOfScale() {
        assertThat(RiskAssessmentLevelResolver.resolve(new BigDecimal("3"))).isEqualTo(RiskAssessmentLevel.MODERATE);
        assertThat(RiskAssessmentLevelResolver.resolve(new BigDecimal("3.000"))).isEqualTo(RiskAssessmentLevel.MODERATE);
        assertThat(RiskAssessmentLevelResolver.resolve(new BigDecimal("8.0000"))).isEqualTo(RiskAssessmentLevel.VERY_HIGH);
    }

    @Test
    @DisplayName("Score negativo cai em LOW")
    void shouldTreatNegativeScoreAsLow() {
        assertThat(RiskAssessmentLevelResolver.resolve(new BigDecimal("-1"))).isEqualTo(RiskAssessmentLevel.LOW);
    }

    @Test
    @DisplayName("É uma classe utilitária: não deve ser instanciável publicamente")
    void shouldNotBeInstantiable() throws Exception {
        Constructor<RiskAssessmentLevelResolver> constructor =
                RiskAssessmentLevelResolver.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }
}
