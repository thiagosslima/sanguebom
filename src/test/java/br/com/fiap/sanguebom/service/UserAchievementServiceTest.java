package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.UserAchievementDTO;
import br.com.fiap.sanguebom.model.entities.Achievement;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.UserAchievement;
import br.com.fiap.sanguebom.model.entities.UserAchievementId;
import br.com.fiap.sanguebom.repository.AchievementRepository;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.UserAchievementRepository;
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
class UserAchievementServiceTest {

    private static final long USER_ID = 5L;
    private static final long ACHIEVEMENT_ID = 9L;
    private static final OffsetDateTime EARNED_AT =
            OffsetDateTime.of(2026, 2, 10, 8, 30, 0, 0, ZoneOffset.UTC);

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AchievementRepository achievementRepository;

    @InjectMocks
    private UserAchievementService userAchievementService;

    private AppUser user() {
        AppUser u = new AppUser();
        u.setId(USER_ID);
        return u;
    }

    private Achievement achievement() {
        Achievement a = new Achievement();
        a.setId(ACHIEVEMENT_ID);
        return a;
    }

    private UserAchievement entity() {
        UserAchievement ua = new UserAchievement();
        ua.setId(new UserAchievementId(USER_ID, ACHIEVEMENT_ID));
        ua.setEarnedAt(EARNED_AT);
        ua.setUser(user());
        ua.setAchievement(achievement());
        return ua;
    }

    private UserAchievementDTO dto(Long userId, Long achievementId) {
        UserAchievementDTO d = new UserAchievementDTO();
        d.setEarnedAt(EARNED_AT);
        d.setUser(userId);
        d.setAchievement(achievementId);
        return d;
    }

    @Test
    @DisplayName("findAll expõe a parte de conquista da chave composta como id do DTO")
    void findAllMapsCompositeKey() {
        when(userAchievementRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(userAchievementService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(ACHIEVEMENT_ID);
            assertThat(d.getEarnedAt()).isEqualTo(EARNED_AT);
            assertThat(d.getUser()).isEqualTo(USER_ID);
            assertThat(d.getAchievement()).isEqualTo(ACHIEVEMENT_ID);
        });
    }

    @Test
    @DisplayName("findAll tolera vínculo sem usuário e sem conquista carregados")
    void findAllToleratesNullRelations() {
        UserAchievement orphan = entity();
        orphan.setUser(null);
        orphan.setAchievement(null);
        when(userAchievementRepository.findAll(Sort.by("id"))).thenReturn(List.of(orphan));

        assertThat(userAchievementService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getUser()).isNull();
            assertThat(d.getAchievement()).isNull();
        });
    }

    @Test
    @DisplayName("get devolve o vínculo existente e lança NotFoundException quando não existe")
    void getBehaviour() {
        when(userAchievementRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(userAchievementService.get(1L).getUser()).isEqualTo(USER_ID);

        when(userAchievementRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userAchievementService.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create resolve usuário e conquista e devolve o id da conquista salva")
    void createResolvesBothRelations() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement()));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenReturn(entity());

        Long id = userAchievementService.create(dto(USER_ID, ACHIEVEMENT_ID));

        ArgumentCaptor<UserAchievement> captor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepository).save(captor.capture());

        assertThat(id).isEqualTo(ACHIEVEMENT_ID);
        assertThat(captor.getValue().getUser().getId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getAchievement().getId()).isEqualTo(ACHIEVEMENT_ID);
        assertThat(captor.getValue().getEarnedAt()).isEqualTo(EARNED_AT);
    }

    @Test
    @DisplayName("create falha quando o usuário informado não existe")
    void createFailsWhenUserMissing() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAchievementService.create(dto(USER_ID, ACHIEVEMENT_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("user");

        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("create falha quando a conquista informada não existe")
    void createFailsWhenAchievementMissing() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAchievementService.create(dto(USER_ID, ACHIEVEMENT_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("achievement");

        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("update aplica o DTO sobre o vínculo existente")
    void updateMutatesExisting() {
        UserAchievement existing = entity();
        OffsetDateTime novaData = EARNED_AT.plusDays(3);
        when(userAchievementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement()));

        UserAchievementDTO d = dto(USER_ID, ACHIEVEMENT_ID);
        d.setEarnedAt(novaData);
        userAchievementService.update(1L, d);

        verify(userAchievementRepository).save(existing);
        assertThat(existing.getEarnedAt()).isEqualTo(novaData);
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(userAchievementRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAchievementService.update(9L, dto(USER_ID, ACHIEVEMENT_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(userAchievementRepository, never()).save(any());
    }
}
