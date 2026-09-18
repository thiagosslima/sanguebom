package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.Achievement;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.entities.UserAchievement;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementRulesTest {

    private static final long USER_ID = 1L;

    @Mock
    private ExamRepository examRepository;

    private AppUser user(HealthProfile profile, AchivementCode... owned) {
        AppUser u = new AppUser();
        u.setId(USER_ID);
        u.setHealthProfile(profile);
        u.setUserAchievements(java.util.Arrays.stream(owned).map(code -> {
            Achievement a = new Achievement();
            a.setCode(code);
            UserAchievement ua = new UserAchievement();
            ua.setAchievement(a);
            return ua;
        }).collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new)));
        return u;
    }

    private HealthProfile profileWith(ExamPeriodicity periodicity) {
        HealthProfile p = new HealthProfile();
        p.setExamPeriodicity(periodicity);
        return p;
    }

    private Exam examCollectedAt(OffsetDateTime when) {
        Exam e = new Exam();
        e.setCollectedAt(when);
        return e;
    }

    private AchievementContext contextOf(AppUser u) {
        return new AchievementContext(u, new Exam(), new RiskAssessment());
    }

    // =================================================================
    @Nested
    @DisplayName("AchievementRuleParent.alreadyHasAchievement")
    class AlreadyHasAchievement {

        @Test
        @DisplayName("Reconhece a conquista que o usuário já possui")
        void detectsOwnedAchievement() {
            var r = new SangueBomAchievementRule(examRepository);
            assertThat(r.alreadyHasAchievement(contextOf(user(null, AchivementCode.SANGUE_BOM)))).isTrue();
        }

        @Test
        @DisplayName("Não confunde conquistas diferentes")
        void doesNotConfuseDifferentAchievements() {
            var r = new SangueBomAchievementRule(examRepository);
            assertThat(r.alreadyHasAchievement(contextOf(user(null, AchivementCode.PUNCTUAL)))).isFalse();
        }

        @Test
        @DisplayName("Usuário sem nenhuma conquista não possui a conquista da regra")
        void handlesUserWithoutAchievements() {
            var r = new SangueBomAchievementRule(examRepository);
            assertThat(r.alreadyHasAchievement(contextOf(user(null)))).isFalse();
        }
    }

    // =================================================================
    @Nested
    @DisplayName("SangueBomAchievementRule: ao menos um exame liberado")
    class SangueBom {

        @Test
        void exposesItsCode() {
            assertThat(new SangueBomAchievementRule(examRepository).getAchievementCode())
                    .isEqualTo(AchivementCode.SANGUE_BOM);
        }

        @Test
        @DisplayName("Inelegível sem nenhum exame liberado")
        void notEligibleWithoutReleasedExams() {
            when(examRepository.countByUserIdAndStatus(USER_ID, ExamStatus.RELEASED)).thenReturn(0L);
            assertThat(new SangueBomAchievementRule(examRepository).isEligible(contextOf(user(null)))).isFalse();
        }

        @Test
        @DisplayName("Elegível a partir do primeiro exame liberado")
        void eligibleFromFirstReleasedExam() {
            when(examRepository.countByUserIdAndStatus(USER_ID, ExamStatus.RELEASED)).thenReturn(1L);
            assertThat(new SangueBomAchievementRule(examRepository).isEligible(contextOf(user(null)))).isTrue();
        }
    }

    // =================================================================
    @Nested
    @DisplayName("HealthChampionAchievementRule: score de risco até 3")
    class HealthChampion {

        private AchievementContext contextWithScore(BigDecimal score) {
            RiskAssessment ra = new RiskAssessment();
            ra.setScore(score);
            return new AchievementContext(user(null), new Exam(), ra);
        }

        @Test
        void exposesItsCode() {
            assertThat(new HealthChampionAchievementRule().getAchievementCode())
                    .isEqualTo(AchivementCode.HEALTH_CHAMPION);
        }

        @Test
        @DisplayName("Inelegível quando não há avaliação de risco")
        void notEligibleWithoutRiskAssessment() {
            var context = new AchievementContext(user(null), new Exam(), null);
            assertThat(new HealthChampionAchievementRule().isEligible(context)).isFalse();
        }

        @Test
        @DisplayName("Inelegível quando a avaliação existe mas não tem score")
        void notEligibleWithoutScore() {
            assertThat(new HealthChampionAchievementRule().isEligible(contextWithScore(null))).isFalse();
        }

        @Test
        @DisplayName("Elegível no limite: score exatamente 3")
        void eligibleAtBoundary() {
            assertThat(new HealthChampionAchievementRule().isEligible(contextWithScore(new BigDecimal("3"))))
                    .isTrue();
            assertThat(new HealthChampionAchievementRule().isEligible(contextWithScore(new BigDecimal("3.00"))))
                    .isTrue();
        }

        @Test
        @DisplayName("Inelegível logo acima do limite")
        void notEligibleJustAboveBoundary() {
            assertThat(new HealthChampionAchievementRule().isEligible(contextWithScore(new BigDecimal("3.01"))))
                    .isFalse();
        }

        @Test
        @DisplayName("Elegível com score bem baixo")
        void eligibleWithLowScore() {
            assertThat(new HealthChampionAchievementRule().isEligible(contextWithScore(BigDecimal.ZERO))).isTrue();
        }
    }

    // =================================================================
    @Nested
    @DisplayName("PunctualAchievementRule: exame seguinte dentro da periodicidade + 30 dias")
    class Punctual {

        private final OffsetDateTime previous = OffsetDateTime.of(
                2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        private void givenLastTwoExams(OffsetDateTime last, OffsetDateTime prev) {
            when(examRepository.findTop2ByUserIdAndStatusOrderByCollectedAtDesc(USER_ID, ExamStatus.RELEASED))
                    .thenReturn(List.of(examCollectedAt(last), examCollectedAt(prev)));
        }

        @Test
        void exposesItsCode() {
            assertThat(new PunctualAchievementRule(examRepository).getAchievementCode())
                    .isEqualTo(AchivementCode.PUNCTUAL);
        }

        @Test
        @DisplayName("Inelegível quando o usuário não tem perfil de saúde")
        void notEligibleWithoutHealthProfile() {
            assertThat(new PunctualAchievementRule(examRepository).isEligible(contextOf(user(null)))).isFalse();
        }

        @Test
        @DisplayName("Inelegível quando o perfil não define periodicidade")
        void notEligibleWithoutPeriodicity() {
            var context = contextOf(user(profileWith(null)));
            assertThat(new PunctualAchievementRule(examRepository).isEligible(context)).isFalse();
        }

        @Test
        @DisplayName("Inelegível com menos de dois exames liberados: não há intervalo a medir")
        void notEligibleWithFewerThanTwoExams() {
            when(examRepository.findTop2ByUserIdAndStatusOrderByCollectedAtDesc(USER_ID, ExamStatus.RELEASED))
                    .thenReturn(List.of(examCollectedAt(previous)));

            var context = contextOf(user(profileWith(ExamPeriodicity.QUARTERLY)));
            assertThat(new PunctualAchievementRule(examRepository).isEligible(context)).isFalse();
        }

        @Test
        @DisplayName("Elegível quando o exame seguinte veio dentro do prazo")
        void eligibleWhenWithinDeadline() {
            // trimestral: 1/1 + 3 meses + 30 dias = 30/4. Exame em 1/3 está dentro.
            givenLastTwoExams(previous.plusMonths(2), previous);

            var context = contextOf(user(profileWith(ExamPeriodicity.QUARTERLY)));
            assertThat(new PunctualAchievementRule(examRepository).isEligible(context)).isTrue();
        }

        @Test
        @DisplayName("Inelegível quando o exame seguinte estourou a tolerância de 30 dias")
        void notEligibleWhenPastTolerance() {
            // trimestral: prazo em 30/4. Exame em 1/6 está fora.
            givenLastTwoExams(previous.plusMonths(5), previous);

            var context = contextOf(user(profileWith(ExamPeriodicity.QUARTERLY)));
            assertThat(new PunctualAchievementRule(examRepository).isEligible(context)).isFalse();
        }

        @Test
        @DisplayName("No dia exato do prazo é inelegível, porque a comparação é isBefore")
        void notEligibleExactlyOnDeadline() {
            // 1/1 + 3 meses = 1/4; + 30 dias = 1/5. Exame exatamente em 1/5.
            givenLastTwoExams(previous.plusMonths(3).plusDays(30), previous);

            var context = contextOf(user(profileWith(ExamPeriodicity.QUARTERLY)));
            assertThat(new PunctualAchievementRule(examRepository).isEligible(context)).isFalse();
        }

        @Test
        @DisplayName("A periodicidade anual dá uma janela maior que a trimestral")
        void yearlyPeriodicityAllowsLongerGap() {
            givenLastTwoExams(previous.plusMonths(11), previous);

            var quarterly = contextOf(user(profileWith(ExamPeriodicity.QUARTERLY)));
            var yearly = contextOf(user(profileWith(ExamPeriodicity.YEARLY)));

            var rule = new PunctualAchievementRule(examRepository);
            assertThat(rule.isEligible(quarterly)).isFalse();
            assertThat(rule.isEligible(yearly)).isTrue();
        }
    }
}
