package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.ReferenceRangeDTO;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceRangeServiceTest {

    private static final long ITEM_ID = 300L;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    @Mock
    private ExamItemRepository examItemRepository;

    @InjectMocks
    private ReferenceRangeService referenceRangeService;

    private ExamItem item() {
        ExamItem i = new ExamItem();
        i.setId(ITEM_ID);
        return i;
    }

    private ReferenceRange entity() {
        ReferenceRange r = new ReferenceRange();
        r.setId(1L);
        r.setSex(Sex.F);
        r.setAgeMinYears(new BigDecimal("18"));
        r.setAgeMaxYears(new BigDecimal("65"));
        r.setVersion("v1");
        r.setSource("SBPC");
        r.setValidFrom(LocalDate.of(2026, 1, 1));
        r.setValidUntil(LocalDate.of(2026, 12, 31));
        r.setExamItem(item());
        return r;
    }

    private ReferenceRangeDTO dto(Long examItemId) {
        ReferenceRangeDTO d = new ReferenceRangeDTO();
        d.setSex(Sex.M);
        d.setAgeMinYears(new BigDecimal("0"));
        d.setAgeMaxYears(new BigDecimal("17"));
        d.setVersion("v2");
        d.setSource("MS");
        d.setValidFrom(LocalDate.of(2027, 1, 1));
        d.setValidUntil(null);
        d.setExamItem(examItemId);
        return d;
    }

    @Test
    @DisplayName("findAll mapeia a faixa e achata o item de exame para o seu id")
    void findAllMapsEveryField() {
        when(referenceRangeRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(referenceRangeService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getSex()).isEqualTo(Sex.F);
            assertThat(d.getAgeMinYears()).isEqualByComparingTo("18");
            assertThat(d.getAgeMaxYears()).isEqualByComparingTo("65");
            assertThat(d.getSource()).isEqualTo("SBPC");
            assertThat(d.getValidFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(d.getValidUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(d.getExamItem()).isEqualTo(ITEM_ID);
        });
    }

    @Test
    @DisplayName("findAll tolera faixa sem item de exame")
    void findAllToleratesNullExamItem() {
        ReferenceRange orphan = entity();
        orphan.setExamItem(null);
        when(referenceRangeRepository.findAll(Sort.by("id"))).thenReturn(List.of(orphan));

        assertThat(referenceRangeService.findAll()).singleElement()
                .extracting(ReferenceRangeDTO::getExamItem).isNull();
    }

    @Test
    @DisplayName("get devolve a faixa existente e lança NotFoundException quando não existe")
    void getBehaviour() {
        when(referenceRangeRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(referenceRangeService.get(1L).getVersion()).isEqualTo("v1");

        when(referenceRangeRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> referenceRangeService.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create resolve o item de exame pelo id antes de salvar")
    void createResolvesExamItem() {
        when(examItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item()));
        ReferenceRange saved = entity();
        saved.setId(66L);
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenReturn(saved);

        Long id = referenceRangeService.create(dto(ITEM_ID));

        ArgumentCaptor<ReferenceRange> captor = ArgumentCaptor.forClass(ReferenceRange.class);
        verify(referenceRangeRepository).save(captor.capture());

        assertThat(id).isEqualTo(66L);
        assertThat(captor.getValue().getExamItem().getId()).isEqualTo(ITEM_ID);
        assertThat(captor.getValue().getSex()).isEqualTo(Sex.M);
        assertThat(captor.getValue().getValidUntil()).isNull();
    }

    @Test
    @DisplayName("create falha quando o item de exame informado não existe")
    void createFailsWhenExamItemMissing() {
        when(examItemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> referenceRangeService.create(dto(ITEM_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("examItem");

        verify(referenceRangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("create aceita DTO sem item de exame")
    void createAcceptsNullExamItem() {
        ReferenceRange saved = entity();
        saved.setId(67L);
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenReturn(saved);

        referenceRangeService.create(dto(null));

        verify(examItemRepository, never()).findById(any());
    }

    @Test
    @DisplayName("update aplica o DTO sobre a faixa existente")
    void updateMutatesExisting() {
        ReferenceRange existing = entity();
        when(referenceRangeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(examItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item()));

        referenceRangeService.update(1L, dto(ITEM_ID));

        verify(referenceRangeRepository).save(existing);
        assertThat(existing.getSex()).isEqualTo(Sex.M);
        assertThat(existing.getVersion()).isEqualTo("v2");
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(referenceRangeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> referenceRangeService.update(9L, dto(ITEM_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(referenceRangeRepository, never()).save(any());
    }
}
