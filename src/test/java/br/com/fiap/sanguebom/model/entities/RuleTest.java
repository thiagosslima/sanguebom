package br.com.fiap.sanguebom.model.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule.appliesTo decide qual faixa classifica um resultado de exame.
 * Um erro de fronteira aqui classifica o paciente errado, então os testes
 * se concentram exatamente nos limites e na semântica de inclusivo/exclusivo.
 */
class RuleTest {

    private Rule rule(String min, Boolean minInclusive, String max, Boolean maxInclusive) {
        Rule rule = new Rule();
        rule.setMinValue(min == null ? null : new BigDecimal(min));
        rule.setMaxValue(max == null ? null : new BigDecimal(max));
        rule.setMinInclusive(minInclusive);
        rule.setMaxInclusive(maxInclusive);
        return rule;
    }

    @Nested
    @DisplayName("Faixa fechada dos dois lados")
    class BoundedRange {

        @ParameterizedTest(name = "valor {0} dentro de [10, 20] -> {1}")
        @CsvSource({
                "9.99,  false",
                "10,    true",
                "15,    true",
                "20,    true",
                "20.01, false"
        })
        void inclusiveOnBothEnds(String value, boolean expected) {
            Rule r = rule("10", true, "20", true);
            assertThat(r.appliesTo(new BigDecimal(value))).isEqualTo(expected);
        }

        @ParameterizedTest(name = "valor {0} dentro de (10, 20) -> {1}")
        @CsvSource({
                "10,    false",
                "10.01, true",
                "19.99, true",
                "20,    false"
        })
        void exclusiveOnBothEnds(String value, boolean expected) {
            Rule r = rule("10", false, "20", false);
            assertThat(r.appliesTo(new BigDecimal(value))).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("Sem limite inferior, qualquer valor abaixo do máximo se aplica")
    void openLowerBound() {
        Rule r = rule(null, null, "20", true);

        assertThat(r.appliesTo(new BigDecimal("-9999"))).isTrue();
        assertThat(r.appliesTo(new BigDecimal("20"))).isTrue();
        assertThat(r.appliesTo(new BigDecimal("20.01"))).isFalse();
    }

    @Test
    @DisplayName("Sem limite superior, qualquer valor acima do mínimo se aplica")
    void openUpperBound() {
        Rule r = rule("10", true, null, null);

        assertThat(r.appliesTo(new BigDecimal("9.99"))).isFalse();
        assertThat(r.appliesTo(new BigDecimal("10"))).isTrue();
        assertThat(r.appliesTo(new BigDecimal("9999"))).isTrue();
    }

    @Test
    @DisplayName("Sem limite algum, a regra é coringa e aceita qualquer valor")
    void unboundedRuleMatchesEverything() {
        Rule r = rule(null, null, null, null);

        assertThat(r.appliesTo(new BigDecimal("-1"))).isTrue();
        assertThat(r.appliesTo(BigDecimal.ZERO)).isTrue();
        assertThat(r.appliesTo(new BigDecimal("1e9"))).isTrue();
    }

    @Test
    @DisplayName("minInclusive nulo é tratado como exclusivo, não como inclusivo")
    void nullInclusiveFlagIsTreatedAsExclusive() {
        Rule r = rule("10", null, "20", null);

        assertThat(r.appliesTo(new BigDecimal("10"))).isFalse();
        assertThat(r.appliesTo(new BigDecimal("20"))).isFalse();
        assertThat(r.appliesTo(new BigDecimal("15"))).isTrue();
    }

    @Test
    @DisplayName("A comparação é numérica, então 10.00 e 10 são o mesmo limite")
    void comparisonIgnoresScale() {
        Rule r = rule("10.00", true, "20.000", true);

        assertThat(r.appliesTo(new BigDecimal("10"))).isTrue();
        assertThat(r.appliesTo(new BigDecimal("20"))).isTrue();
    }

    @Test
    @DisplayName("Faixa invertida (min maior que max) não se aplica a nenhum valor")
    void invertedRangeNeverApplies() {
        Rule r = rule("20", true, "10", true);

        assertThat(r.appliesTo(new BigDecimal("15"))).isFalse();
        assertThat(r.appliesTo(new BigDecimal("20"))).isFalse();
        assertThat(r.appliesTo(new BigDecimal("10"))).isFalse();
    }
}
