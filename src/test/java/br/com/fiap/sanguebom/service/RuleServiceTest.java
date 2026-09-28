package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.RuleDTO;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.entities.Rule;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import br.com.fiap.sanguebom.repository.RuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    private static final long RANGE_ID = 500L;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    @InjectMocks
    private RuleService ruleService;

    private ReferenceRange range() {
        ReferenceRange r = new ReferenceRange();
        r.setId(RANGE_ID);
        return r;
    }

    private Rule entity() {
        Rule rule = new Rule();
        rule.setId(1L);
        rule.setMinValue(new BigDecimal("1.0000"));
        rule.setMaxValue(new BigDecimal("9.0000"));
        rule.setMinInclusive(true);
        rule.setMaxInclusive(false);
        rule.setLevel(ExamResultFlag.NORMAL);
        rule.setScore(new BigDecimal("2.50"));
        rule.setDescription("faixa normal");
        rule.setVersion("v1");
        rule.setActive(true);
        rule.setReferenceRange(range());
        return rule;
    }

    private RuleDTO dto(Long referenceRangeId) {
        RuleDTO d = new RuleDTO();
        d.setMinValue(new BigDecimal("10.0000"));
        d.setMaxValue(new BigDecimal("20.0000"));
        d.setMinInclusive(false);
        d.setMaxInclusive(true);
        d.setLevel(ExamResultFlag.HIGH);
        d.setScore(new BigDecimal("7.00"));
        d.setDescription("faixa alta");
        d.setVersion("v2");
        d.setActive(true);
        d.setReferenceRange(referenceRangeId);
        return d;
    }

    @Test
    @DisplayName("findAll achata a faixa de referência para o seu id")
    void findAllFlattensReferenceRangeToId() {
        when(ruleRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(ruleService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getMinValue()).isEqualByComparingTo("1.0000");
            assertThat(d.getMaxValue()).isEqualByComparingTo("9.0000");
            assertThat(d.getMinInclusive()).isTrue();
            assertThat(d.getMaxInclusive()).isFalse();
            assertThat(d.getLevel()).isEqualTo(ExamResultFlag.NORMAL);
            assertThat(d.getScore()).isEqualByComparingTo("2.50");
            assertThat(d.getVersion()).isEqualTo("v1");
            assertThat(d.getReferenceRange()).isEqualTo(RANGE_ID);
        });
    }

    @Test
    @DisplayName("findAll tolera regra sem faixa de referência, devolvendo null no campo")
    void findAllToleratesNullReferenceRange() {
        Rule orphan = entity();
        orphan.setReferenceRange(null);
        when(ruleRepository.findAll(Sort.by("id"))).thenReturn(List.of(orphan));

        assertThat(ruleService.findAll()).singleElement()
                .extracting(RuleDTO::getReferenceRange).isNull();
    }

    @Test
    @DisplayName("get lança NotFoundException para id inexistente")
    void getThrowsWhenMissing() {
        when(ruleRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ruleService.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create resolve a faixa de referência pelo id antes de salvar")
    void createResolvesReferenceRange() {
        when(referenceRangeRepository.findById(RANGE_ID)).thenReturn(Optional.of(range()));
        Rule saved = entity();
        saved.setId(88L);
        when(ruleRepository.save(any(Rule.class))).thenReturn(saved);

        Long id = ruleService.create(dto(RANGE_ID));

        ArgumentCaptor<Rule> captor = ArgumentCaptor.forClass(Rule.class);
        verify(ruleRepository).save(captor.capture());

        assertThat(id).isEqualTo(88L);
        assertThat(captor.getValue().getReferenceRange().getId()).isEqualTo(RANGE_ID);
        assertThat(captor.getValue().getLevel()).isEqualTo(ExamResultFlag.HIGH);
        assertThat(captor.getValue().getMinInclusive()).isFalse();
    }

    @Test
    @DisplayName("create aceita DTO sem faixa de referência e nem consulta o repositório")
    void createAcceptsNullReferenceRange() {
        Rule saved = entity();
        saved.setId(89L);
        when(ruleRepository.save(any(Rule.class))).thenReturn(saved);

        ruleService.create(dto(null));

        ArgumentCaptor<Rule> captor = ArgumentCaptor.forClass(Rule.class);
        verify(ruleRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceRange()).isNull();
        verify(referenceRangeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("create falha quando a faixa de referência informada não existe")
    void createFailsWhenReferenceRangeMissing() {
        when(referenceRangeRepository.findById(RANGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.create(dto(RANGE_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("referenceRange");

        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("update aplica o DTO sobre a regra existente")
    void updateMutatesExisting() {
        Rule existing = entity();
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(referenceRangeRepository.findById(RANGE_ID)).thenReturn(Optional.of(range()));

        ruleService.update(1L, dto(RANGE_ID));

        verify(ruleRepository).save(existing);
        assertThat(existing.getLevel()).isEqualTo(ExamResultFlag.HIGH);
        assertThat(existing.getScore()).isEqualByComparingTo("7.00");
        assertThat(existing.getVersion()).isEqualTo("v2");
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(ruleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.update(9L, dto(RANGE_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(ruleRepository, never()).save(any());
    }
}
