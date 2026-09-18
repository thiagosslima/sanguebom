package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.HealthUnitDTO;
import br.com.fiap.sanguebom.model.entities.HealthUnit;
import br.com.fiap.sanguebom.repository.HealthUnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthUnitServiceTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private HealthUnitRepository healthUnitRepository;

    @InjectMocks
    private HealthUnitService healthUnitService;

    private HealthUnit entity() {
        HealthUnit h = new HealthUnit();
        h.setId(1L);
        h.setName("UBS Central");
        h.setCnes("1234567");
        h.setType("UBS");
        h.setStatus("ACTIVE");
        h.setCreatedAt(NOW);
        return h;
    }

    private HealthUnitDTO dto() {
        HealthUnitDTO d = new HealthUnitDTO();
        d.setName("UBS Norte");
        d.setCnes("7654321");
        d.setType("HOSPITAL");
        d.setStatus("INACTIVE");
        d.setCreatedAt(NOW);
        return d;
    }

    @Test
    @DisplayName("findAll mapeia todos os campos da unidade de saúde")
    void findAllMapsEveryField() {
        when(healthUnitRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(healthUnitService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getName()).isEqualTo("UBS Central");
            assertThat(d.getCnes()).isEqualTo("1234567");
            assertThat(d.getType()).isEqualTo("UBS");
            assertThat(d.getStatus()).isEqualTo("ACTIVE");
            assertThat(d.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("get devolve a unidade existente")
    void getReturnsExisting() {
        when(healthUnitRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(healthUnitService.get(1L).getCnes()).isEqualTo("1234567");
    }

    @Test
    @DisplayName("get lança NotFoundException para id inexistente")
    void getThrowsWhenMissing() {
        when(healthUnitRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> healthUnitService.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create persiste os campos do DTO e devolve o id gerado")
    void createPersistsMappedEntity() {
        HealthUnit saved = entity();
        saved.setId(55L);
        when(healthUnitRepository.save(any(HealthUnit.class))).thenReturn(saved);

        Long id = healthUnitService.create(dto());

        ArgumentCaptor<HealthUnit> captor = ArgumentCaptor.forClass(HealthUnit.class);
        verify(healthUnitRepository).save(captor.capture());

        assertThat(id).isEqualTo(55L);
        assertThat(captor.getValue().getName()).isEqualTo("UBS Norte");
        assertThat(captor.getValue().getCnes()).isEqualTo("7654321");
        assertThat(captor.getValue().getType()).isEqualTo("HOSPITAL");
    }

    @Test
    @DisplayName("update aplica o DTO sobre a entidade existente")
    void updateMutatesExisting() {
        HealthUnit existing = entity();
        when(healthUnitRepository.findById(1L)).thenReturn(Optional.of(existing));

        healthUnitService.update(1L, dto());

        verify(healthUnitRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("UBS Norte");
        assertThat(existing.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(healthUnitRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthUnitService.update(9L, dto()))
                .isInstanceOf(NotFoundException.class);

        verify(healthUnitRepository, never()).save(any());
    }
}
