package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.DuplicatedAchievementException;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.AchievementDTO;
import br.com.fiap.sanguebom.model.entities.Achievement;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.repository.AchievementRepository;
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
class AchievementServiceTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private AchievementRepository achievementRepository;

    @InjectMocks
    private AchievementService achievementService;

    private Achievement entity() {
        Achievement a = new Achievement();
        a.setId(1L);
        a.setCode(AchivementCode.SANGUE_BOM);
        a.setName("Sangue Bom");
        a.setDescription("Primeiro exame liberado");
        a.setActive(true);
        a.setCreatedAt(NOW);
        return a;
    }

    private AchievementDTO dto() {
        AchievementDTO d = new AchievementDTO();
        d.setCode(AchivementCode.PUNCTUAL);
        d.setName("Pontual");
        d.setDescription("Exame em dia");
        d.setActive(false);
        d.setCreatedAt(NOW);
        return d;
    }

    @Test
    @DisplayName("findAll mapeia o código da conquista como enum, não como texto solto")
    void findAllMapsEveryField() {
        when(achievementRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(achievementService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getCode()).isEqualTo(AchivementCode.SANGUE_BOM);
            assertThat(d.getName()).isEqualTo("Sangue Bom");
            assertThat(d.getDescription()).isEqualTo("Primeiro exame liberado");
            assertThat(d.getActive()).isTrue();
            assertThat(d.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("get devolve a conquista existente")
    void getReturnsExisting() {
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(achievementService.get(1L).getCode()).isEqualTo(AchivementCode.SANGUE_BOM);
    }

    @Test
    @DisplayName("get lança NotFoundException para id inexistente")
    void getThrowsWhenMissing() {
        when(achievementRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> achievementService.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create persiste os campos do DTO e devolve o id gerado")
    void createPersistsMappedEntity() {
        Achievement saved = entity();
        saved.setId(33L);
        when(achievementRepository.save(any(Achievement.class))).thenReturn(saved);

        Long id = achievementService.create(dto());

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository).save(captor.capture());

        assertThat(id).isEqualTo(33L);
        assertThat(captor.getValue().getCode()).isEqualTo(AchivementCode.PUNCTUAL);
        assertThat(captor.getValue().getActive()).isFalse();
    }

    @Test
    @DisplayName("update aplica o DTO sobre a entidade existente")
    void updateMutatesExisting() {
        Achievement existing = entity();
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(existing));

        achievementService.update(1L, dto());

        verify(achievementRepository).save(existing);
        assertThat(existing.getCode()).isEqualTo(AchivementCode.PUNCTUAL);
        assertThat(existing.getName()).isEqualTo("Pontual");
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(achievementRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achievementService.update(9L, dto()))
                .isInstanceOf(NotFoundException.class);

        verify(achievementRepository, never()).save(any());
    }

    // ------------------------------------------------------------ unicidade do code
    //
    // Com duas linhas do mesmo code, o findByCode usado pelo motor de conquistas levanta
    // IncorrectResultSizeDataAccessException e o POST /api/exams passa a responder 500.

    @Test
    @DisplayName("create recusa um code ja cadastrado")
    void createRejectsDuplicatedCode() {
        when(achievementRepository.existsByCode(AchivementCode.PUNCTUAL)).thenReturn(true);

        assertThatThrownBy(() -> achievementService.create(dto()))
                .isInstanceOf(DuplicatedAchievementException.class)
                .hasMessageContaining("PUNCTUAL");

        verify(achievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("update exclui a propria conquista da checagem de unicidade")
    void updateIgnoresOwnCode() {
        Achievement existing = entity();
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(achievementRepository.existsByCodeAndIdNot(AchivementCode.PUNCTUAL, 1L))
                .thenReturn(false);

        achievementService.update(1L, dto());

        verify(achievementRepository).save(existing);
    }

    @Test
    @DisplayName("update recusa o code de outra conquista")
    void updateRejectsCodeOfAnotherAchievement() {
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(entity()));
        when(achievementRepository.existsByCodeAndIdNot(AchivementCode.PUNCTUAL, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> achievementService.update(1L, dto()))
                .isInstanceOf(DuplicatedAchievementException.class);

        verify(achievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("conquista sem code nao dispara checagem de unicidade")
    void skipsUniquenessWhenCodeIsAbsent() {
        AchievementDTO semCode = dto();
        semCode.setCode(null);
        when(achievementRepository.save(any(Achievement.class))).thenReturn(entity());

        achievementService.create(semCode);

        verify(achievementRepository, never()).existsByCode(any());
    }

    @Test
    @DisplayName("o conflito de code vira 422, com titulo proprio")
    void duplicatedAchievementIsUnprocessableEntity() {
        var problem = new DuplicatedAchievementException("Já existe uma conquista cadastrada")
                .toProblemDetail();

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getTitle()).isEqualTo("Conquista já cadastrada");
        assertThat(problem.getDetail()).isEqualTo("Já existe uma conquista cadastrada");
    }
}
