package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.mapper.ExamItemCatalogMapper;
import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.dtos.ExamItemDTO;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamItemServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final Clock clock = Clock.fixed(
            TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @Mock
    private ExamItemRepository examItemRepository;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private ExamItemCatalogMapper examItemCatalogMapper;

    private ExamItemService service() {
        return new ExamItemService(examItemRepository, referenceRangeRepository,
                publisher, clock, examItemCatalogMapper);
    }

    private ExamItem entity() {
        ExamItem i = new ExamItem();
        i.setId(1L);
        i.setCode("HB");
        i.setName("Hemoglobina");
        i.setUnit("g/dL");
        i.setCategory("HEMATOLOGIA");
        i.setDescription("Dosagem de hemoglobina");
        i.setActive(true);
        i.setCreatedAt(NOW);
        return i;
    }

    private ExamItemDTO dto() {
        ExamItemDTO d = new ExamItemDTO();
        d.setCode("GLI");
        d.setName("Glicose");
        d.setUnit("mg/dL");
        d.setCategory("BIOQUIMICA");
        d.setDescription("Glicemia de jejum");
        d.setActive(false);
        d.setCreatedAt(NOW);
        return d;
    }

    private ExamItemCatalogDTO catalogDTO() {
        return new ExamItemCatalogDTO(1L, "HB", "Hemoglobina", "g/dL", "HEMATOLOGIA", "desc");
    }

    // --- catálogo paginado ----------------------------------------------

    @Test
    @DisplayName("Sem categoria, busca o catálogo inteiro ordenado por nome")
    void catalogWithoutCategoryQueriesAllItems() {
        Page<ExamItem> page = new PageImpl<>(List.of(entity()), PageRequest.of(0, 20), 1);
        when(examItemRepository.findActiveCatalogItems(any(Pageable.class))).thenReturn(page);
        when(examItemCatalogMapper.toCatalogDTO(any())).thenReturn(catalogDTO());

        PageResponse<ExamItemCatalogDTO> result = service().findActiveCatalogItems(null, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(examItemRepository).findActiveCatalogItems(captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("name").ascending());

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("Categoria em branco é tratada como ausente, não como filtro vazio")
    void blankCategoryIsTreatedAsAbsent() {
        Page<ExamItem> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(examItemRepository.findActiveCatalogItems(any(Pageable.class))).thenReturn(page);

        service().findActiveCatalogItems("   ", 0, 10);

        verify(examItemRepository).findActiveCatalogItems(any(Pageable.class));
        verify(examItemRepository, never()).findActiveCatalogItemsByCategory(any(), any());
    }

    @Test
    @DisplayName("Categoria informada é filtrada e vai sem espaços em volta")
    void categoryIsTrimmedBeforeQuery() {
        Page<ExamItem> page = new PageImpl<>(List.of(entity()), PageRequest.of(1, 5), 6);
        when(examItemRepository.findActiveCatalogItemsByCategory(eq("HEMATOLOGIA"), any(Pageable.class)))
                .thenReturn(page);
        when(examItemCatalogMapper.toCatalogDTO(any())).thenReturn(catalogDTO());

        PageResponse<ExamItemCatalogDTO> result =
                service().findActiveCatalogItems("  HEMATOLOGIA  ", 1, 5);

        verify(examItemRepository).findActiveCatalogItemsByCategory(eq("HEMATOLOGIA"), any(Pageable.class));
        verify(examItemRepository, never()).findActiveCatalogItems(any());
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    // --- faixas de referência vigentes -----------------------------------

    @Test
    @DisplayName("As faixas vigentes são consultadas na data do clock injetado, não em LocalDate.now()")
    void currentRangesUseInjectedClock() {
        when(examItemRepository.findActiveByCode("HB")).thenReturn(Optional.of(entity()));
        when(referenceRangeRepository.findCurrentRangesByExamItemId(1L, TODAY))
                .thenReturn(List.of(new ReferenceRange()));
        when(examItemCatalogMapper.toReferenceRangeCatalogDTO(any()))
                .thenReturn(new ReferenceRangeCatalogDTO(1L, null, null, null, null, null, null, null, List.of()));

        List<ReferenceRangeCatalogDTO> result = service().findCurrentReferenceRanges("HB");

        assertThat(result).hasSize(1);
        verify(referenceRangeRepository).findCurrentRangesByExamItemId(1L, TODAY);
    }

    @Test
    @DisplayName("Código de item inativo ou inexistente gera NotFoundException citando o código")
    void currentRangesFailForUnknownCode() {
        when(examItemRepository.findActiveByCode("XPTO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findCurrentReferenceRanges("XPTO"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("XPTO");
    }

    // --- CRUD -------------------------------------------------------------

    @Test
    @DisplayName("findAll mapeia todos os campos do item")
    void findAllMapsEveryField() {
        when(examItemRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(service().findAll()).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getCode()).isEqualTo("HB");
            assertThat(d.getName()).isEqualTo("Hemoglobina");
            assertThat(d.getUnit()).isEqualTo("g/dL");
            assertThat(d.getCategory()).isEqualTo("HEMATOLOGIA");
            assertThat(d.getActive()).isTrue();
            assertThat(d.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("get devolve o item existente e lança NotFoundException quando não existe")
    void getBehaviour() {
        when(examItemRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(service().get(1L).getCode()).isEqualTo("HB");

        when(examItemRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create persiste os campos do DTO e devolve o id gerado")
    void createPersistsMappedEntity() {
        ExamItem saved = entity();
        saved.setId(11L);
        when(examItemRepository.save(any(ExamItem.class))).thenReturn(saved);

        Long id = service().create(dto());

        ArgumentCaptor<ExamItem> captor = ArgumentCaptor.forClass(ExamItem.class);
        verify(examItemRepository).save(captor.capture());

        assertThat(id).isEqualTo(11L);
        assertThat(captor.getValue().getCode()).isEqualTo("GLI");
        assertThat(captor.getValue().getActive()).isFalse();
    }

    @Test
    @DisplayName("update aplica o DTO sobre o item existente")
    void updateMutatesExisting() {
        ExamItem existing = entity();
        when(examItemRepository.findById(1L)).thenReturn(Optional.of(existing));

        service().update(1L, dto());

        verify(examItemRepository).save(existing);
        assertThat(existing.getCode()).isEqualTo("GLI");
        assertThat(existing.getName()).isEqualTo("Glicose");
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(examItemRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(9L, dto()))
                .isInstanceOf(NotFoundException.class);

        verify(examItemRepository, never()).save(any());
    }
}
