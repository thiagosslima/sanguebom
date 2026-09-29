package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.Achievement;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.entities.UserAchievement;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.repository.AchievementRepository;
import br.com.fiap.sanguebom.repository.UserAchievementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementEngineTest {

    private static final long USER_ID = 42L;
    private static final long ACHIEVEMENT_ID = 7L;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private AchievementRule rule;

    private AppUser user;
    private Exam exam;
    private RiskAssessment riskAssessment;

    private AchievementEngine engineWith(AchievementRule... rules) {
        user = new AppUser();
        user.setId(USER_ID);
        exam = new Exam();
        riskAssessment = new RiskAssessment();
        return new AchievementEngine(List.of(rules), achievementRepository, userAchievementRepository);
    }

    private Achievement achievement(AchivementCode code) {
        Achievement a = new Achievement();
        a.setId(ACHIEVEMENT_ID);
        a.setCode(code);
        return a;
    }

    @Test
    @DisplayName("Concede a conquista quando a regra é elegível e o usuário ainda não a tem")
    void shouldGrantAchievementWhenEligible() {
        AchievementEngine engine = engineWith(rule);
        when(rule.alreadyHasAchievement(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(rule.isEligible(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(rule.getAchievementCode()).thenReturn(AchivementCode.SANGUE_BOM);
        when(achievementRepository.findByCode(AchivementCode.SANGUE_BOM))
                .thenReturn(Optional.of(achievement(AchivementCode.SANGUE_BOM)));

        engine.evaluate(user, exam, riskAssessment);

        ArgumentCaptor<UserAchievement> captor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepository).save(captor.capture());

        UserAchievement saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getAchievement().getCode()).isEqualTo(AchivementCode.SANGUE_BOM);
        assertThat(saved.getId().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getId().getAchievementId()).isEqualTo(ACHIEVEMENT_ID);
    }

    @Test
    @DisplayName("Não concede de novo uma conquista que o usuário já possui, e nem avalia a elegibilidade")
    void shouldSkipRuleWhenUserAlreadyHasAchievement() {
        AchievementEngine engine = engineWith(rule);
        when(rule.alreadyHasAchievement(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        engine.evaluate(user, exam, riskAssessment);

        verify(rule, never()).isEligible(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userAchievementRepository, achievementRepository);
    }

    @Test
    @DisplayName("Não concede nada quando a regra considera o usuário inelegível")
    void shouldNotGrantWhenNotEligible() {
        AchievementEngine engine = engineWith(rule);
        when(rule.alreadyHasAchievement(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(rule.isEligible(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        engine.evaluate(user, exam, riskAssessment);

        verifyNoInteractions(userAchievementRepository, achievementRepository);
    }

    @Test
    @DisplayName("Uma regra inelegível não impede que a seguinte conceda a sua conquista")
    void shouldKeepEvaluatingRemainingRules() {
        AchievementRule ineligible = org.mockito.Mockito.mock(AchievementRule.class);
        AchievementRule eligible = org.mockito.Mockito.mock(AchievementRule.class);

        AchievementEngine engine = engineWith(ineligible, eligible);

        when(ineligible.alreadyHasAchievement(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(ineligible.isEligible(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        when(eligible.alreadyHasAchievement(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(eligible.isEligible(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(eligible.getAchievementCode()).thenReturn(AchivementCode.PUNCTUAL);
        when(achievementRepository.findByCode(AchivementCode.PUNCTUAL))
                .thenReturn(Optional.of(achievement(AchivementCode.PUNCTUAL)));

        engine.evaluate(user, exam, riskAssessment);

        verify(userAchievementRepository).save(org.mockito.ArgumentMatchers.any(UserAchievement.class));
    }

    @Test
    @DisplayName("Falha quando a regra aponta para um código de conquista que não existe no banco")
    void shouldFailWhenAchievementIsNotConfigured() {
        AchievementEngine engine = engineWith(rule);
        when(rule.alreadyHasAchievement(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(rule.isEligible(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(rule.getAchievementCode()).thenReturn(AchivementCode.HEALTH_CHAMPION);
        when(achievementRepository.findByCode(AchivementCode.HEALTH_CHAMPION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.evaluate(user, exam, riskAssessment))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("HEALTH_CHAMPION");

        verify(userAchievementRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Sem regras registradas, a avaliação não faz nada")
    void shouldDoNothingWithoutRules() {
        AchievementEngine engine = engineWith();

        engine.evaluate(user, exam, riskAssessment);

        verifyNoInteractions(userAchievementRepository, achievementRepository);
    }
}
