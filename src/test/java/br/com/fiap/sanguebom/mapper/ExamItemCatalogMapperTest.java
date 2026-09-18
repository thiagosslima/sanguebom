package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRuleCatalogDTO;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.entities.Rule;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O mapper do catálogo não é um mapeamento burro: ele filtra regras inativas
 * e ordena as ativas por minValue, com nulos primeiro. É isso que o app
 * mobile usa para desenhar a régua de faixas, então a ordem importa.
 */
class ExamItemCatalogMapperTest {

    private final ExamItemCatalogMapper mapper = new ExamItemCatalogMapper();

    private Rule rule(Long id, String minValue, boolean active) {
        Rule r = new Rule();
        r.setId(id);
        r.setMinValue(minValue == null ? null : new BigDecimal(minValue));
        r.setMaxValue(new BigDecimal("999"));
        r.setMinInclusive(true);
        r.setMaxInclusive(false);
        r.setLevel(ExamResultFlag.NORMAL);
        r.setScore(new BigDecimal("1.00"));
        r.setDescription("regra " + id);
        r.setVersion("v1");
        r.setActive(active);
        return r;
    }

    private ReferenceRange rangeWith(Rule... rules) {
        ReferenceRange range = new ReferenceRange();
        range.setId(1L);
        range.setSex(Sex.F);
        range.setAgeMinYears(new BigDecimal("18"));
        range.setAgeMaxYears(new BigDecimal("65"));
        range.setVersion("v1");
        range.setSource("SBPC");
        range.setValidFrom(LocalDate.of(2026, 1, 1));
        range.setValidUntil(LocalDate.of(2026, 12, 31));
        range.setReferenceRangeRules(new LinkedHashSet<>(Set.of(rules)));
        return range;
    }

    @Test
    @DisplayName("toCatalogDTO copia os campos públicos do item de exame")
    void mapsExamItemFields() {
        ExamItem item = new ExamItem();
        item.setId(3L);
        item.setCode("HB");
        item.setName("Hemoglobina");
        item.setUnit("g/dL");
        item.setCategory("HEMATOLOGIA");
        item.setDescription("Dosagem");

        var dto = mapper.toCatalogDTO(item);

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.code()).isEqualTo("HB");
        assertThat(dto.name()).isEqualTo("Hemoglobina");
        assertThat(dto.unit()).isEqualTo("g/dL");
        assertThat(dto.category()).isEqualTo("HEMATOLOGIA");
        assertThat(dto.description()).isEqualTo("Dosagem");
    }

    @Test
    @DisplayName("Regras inativas ficam de fora do catálogo")
    void filtersOutInactiveRules() {
        var dto = mapper.toReferenceRangeCatalogDTO(rangeWith(
                rule(1L, "0", true),
                rule(2L, "10", false),
                rule(3L, "20", true)));

        assertThat(dto.rules()).extracting(ReferenceRuleCatalogDTO::id)
                .containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("Regra com active nulo é tratada como inativa")
    void treatsNullActiveAsInactive() {
        Rule semFlag = rule(9L, "5", true);
        semFlag.setActive(null);

        var dto = mapper.toReferenceRangeCatalogDTO(rangeWith(semFlag, rule(1L, "0", true)));

        assertThat(dto.rules()).extracting(ReferenceRuleCatalogDTO::id).containsExactly(1L);
    }

    @Test
    @DisplayName("As regras saem ordenadas por minValue crescente")
    void sortsRulesByMinValue() {
        var dto = mapper.toReferenceRangeCatalogDTO(rangeWith(
                rule(3L, "20", true),
                rule(1L, "0", true),
                rule(2L, "10", true)));

        assertThat(dto.rules()).extracting(ReferenceRuleCatalogDTO::id)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Regra sem limite inferior vem primeiro, não estoura NullPointerException")
    void putsNullMinValueFirst() {
        var dto = mapper.toReferenceRangeCatalogDTO(rangeWith(
                rule(2L, "10", true),
                rule(1L, null, true)));

        assertThat(dto.rules()).extracting(ReferenceRuleCatalogDTO::id)
                .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Faixa sem nenhuma regra ativa devolve lista vazia, não nula")
    void emptyRulesProduceEmptyList() {
        ReferenceRangeCatalogDTO dto = mapper.toReferenceRangeCatalogDTO(rangeWith(rule(1L, "0", false)));

        assertThat(dto.rules()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Os metadados da faixa são copiados junto com as regras")
    void mapsRangeMetadata() {
        var dto = mapper.toReferenceRangeCatalogDTO(rangeWith(rule(1L, "0", true)));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.sex()).isEqualTo(Sex.F);
        assertThat(dto.ageMinYears()).isEqualByComparingTo("18");
        assertThat(dto.ageMaxYears()).isEqualByComparingTo("65");
        assertThat(dto.version()).isEqualTo("v1");
        assertThat(dto.source()).isEqualTo("SBPC");
        assertThat(dto.validFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(dto.validUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}
