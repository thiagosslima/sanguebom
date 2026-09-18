package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.HealthProfileDTO;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.HealthProfileRepository;
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
class HealthProfileServiceTest {

    private static final long USER_ID = 200L;

    @Mock
    private HealthProfileRepository healthProfileRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private HealthProfileService healthProfileService;

    private AppUser user() {
        AppUser u = new AppUser();
        u.setId(USER_ID);
        return u;
    }

    private HealthProfile entity() {
        HealthProfile p = new HealthProfile();
        p.setId(1L);
        p.setSex(Sex.F);
        p.setHeightCm(new BigDecimal("165.0"));
        p.setWeightKg(new BigDecimal("62.5"));
        p.setRiskFactors("tabagismo");
        p.setExamPeriodicity(ExamPeriodicity.SEMESTERLY);
        p.setUser(user());
        return p;
    }

    private HealthProfileDTO dto(Long userId) {
        HealthProfileDTO d = new HealthProfileDTO();
        d.setSex(Sex.M);
        d.setHeightCm(new BigDecimal("180.0"));
        d.setWeightKg(new BigDecimal("80.0"));
        d.setRiskFactors("sedentarismo");
        d.setExamPeriodicity(ExamPeriodicity.YEARLY);
        d.setUser(userId);
        return d;
    }

    @Test
    @DisplayName("findAll achata o usuário para o seu id")
    void findAllMapsEveryField() {
        when(healthProfileRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(healthProfileService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getSex()).isEqualTo(Sex.F);
            assertThat(d.getHeightCm()).isEqualByComparingTo("165.0");
            assertThat(d.getWeightKg()).isEqualByComparingTo("62.5");
            assertThat(d.getRiskFactors()).isEqualTo("tabagismo");
            assertThat(d.getExamPeriodicity()).isEqualTo(ExamPeriodicity.SEMESTERLY);
            assertThat(d.getUser()).isEqualTo(USER_ID);
        });
    }

    @Test
    @DisplayName("get devolve o perfil existente e lança NotFoundException quando não existe")
    void getBehaviour() {
        when(healthProfileRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(healthProfileService.get(1L).getSex()).isEqualTo(Sex.F);

        when(healthProfileRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> healthProfileService.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getByUserId devolve a entidade, não o DTO, porque é consumida internamente")
    void getByUserIdReturnsEntity() {
        when(healthProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(entity()));

        HealthProfile profile = healthProfileService.getByUserId(USER_ID);

        assertThat(profile.getExamPeriodicity()).isEqualTo(ExamPeriodicity.SEMESTERLY);
    }

    @Test
    @DisplayName("getByUserId inclui o id do usuário na mensagem de erro")
    void getByUserIdThrowsWithUserIdInMessage() {
        when(healthProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthProfileService.getByUserId(USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(String.valueOf(USER_ID));
    }

    @Test
    @DisplayName("create resolve o usuário pelo id antes de salvar")
    void createResolvesUser() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        HealthProfile saved = entity();
        saved.setId(22L);
        when(healthProfileRepository.save(any(HealthProfile.class))).thenReturn(saved);

        Long id = healthProfileService.create(dto(USER_ID));

        ArgumentCaptor<HealthProfile> captor = ArgumentCaptor.forClass(HealthProfile.class);
        verify(healthProfileRepository).save(captor.capture());

        assertThat(id).isEqualTo(22L);
        assertThat(captor.getValue().getUser().getId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getExamPeriodicity()).isEqualTo(ExamPeriodicity.YEARLY);
    }

    @Test
    @DisplayName("create falha quando o usuário informado não existe")
    void createFailsWhenUserMissing() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthProfileService.create(dto(USER_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("user");

        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("update aplica o DTO sobre o perfil existente")
    void updateMutatesExisting() {
        HealthProfile existing = entity();
        when(healthProfileRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

        healthProfileService.update(1L, dto(USER_ID));

        verify(healthProfileRepository).save(existing);
        assertThat(existing.getSex()).isEqualTo(Sex.M);
        assertThat(existing.getRiskFactors()).isEqualTo("sedentarismo");
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(healthProfileRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthProfileService.update(9L, dto(USER_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(healthProfileRepository, never()).save(any());
    }
}
