package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.enums.ExamGoalStatus;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import br.com.fiap.sanguebom.model.userexam.ExamGoalDTO;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * CF-331 / CF-382 / CF-384 - calculo do vencimento da meta e as situacoes possiveis.
 */
@ExtendWith(MockitoExtension.class)
class ExamGoalServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 28);
    private static final ZoneId ZONE = ZoneOffset.UTC;

    @Mock
    private UserServiceHelper userServiceHelper;

    @Mock
    private HealthProfileService healthProfileService;

    @Mock
    private ExamRepository examRepository;

    private ExamGoalService service;

    @BeforeEach
    void setUp() {
        final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
        service = new ExamGoalService(userServiceHelper, healthProfileService, examRepository, clock);
    }

    @Test
    @DisplayName("exame recente e periodicidade anual: meta em dia com a data do ultimo exame e o vencimento")
    void shouldReturnUpToDate() {
        givenProfile("YEARLY");
        givenLastExamCollectedAt(TODAY.minusMonths(2));

        final ExamGoalDTO goal = service.goalOf(USER_ID);

        assertThat(goal.lastExamDate()).isEqualTo(LocalDate.of(2026, 6, 28));
        assertThat(goal.dueDate()).isEqualTo(LocalDate.of(2027, 6, 28));
        assertThat(goal.daysRemaining()).isEqualTo(304L);
        assertThat(goal.periodicity()).isEqualTo(ExamPeriodicity.YEARLY);
        assertThat(goal.status()).isEqualTo(ExamGoalStatus.UP_TO_DATE);
    }

    @ParameterizedTest(name = "vencimento em {0} dia(s) -> {1}")
    @DisplayName("limites da janela de 30 dias que separa UP_TO_DATE de DUE_SOON")
    @CsvSource({
            "31,  UP_TO_DATE",
            "30,  DUE_SOON",
            "1,   DUE_SOON",
            "0,   DUE_SOON",
            "-1,  OVERDUE",
            "-90, OVERDUE"
    })
    void shouldClassifyGoalWindowBoundaries(final long daysRemaining, final ExamGoalStatus expected) {
        givenProfile("YEARLY");
        // vencimento = coleta + 12 meses, entao a coleta e retrocedida para cair no dia desejado
        givenLastExamCollectedAt(TODAY.plusDays(daysRemaining).minusMonths(12));

        final ExamGoalDTO goal = service.goalOf(USER_ID);

        assertThat(goal.daysRemaining()).isEqualTo(daysRemaining);
        assertThat(goal.status()).isEqualTo(expected);
    }

    @Test
    @DisplayName("cidadao sem nenhum exame: situacao NO_HISTORY e datas nulas")
    void shouldReturnNoHistoryWhenCitizenHasNoExam() {
        givenProfile("YEARLY");
        given(examRepository.findFirstByUserIdOrderByCollectedAtDesc(USER_ID)).willReturn(Optional.empty());

        final ExamGoalDTO goal = service.goalOf(USER_ID);

        assertThat(goal.status()).isEqualTo(ExamGoalStatus.NO_HISTORY);
        assertThat(goal.lastExamDate()).isNull();
        assertThat(goal.dueDate()).isNull();
        assertThat(goal.daysRemaining()).isNull();
        assertThat(goal.periodicity()).isEqualTo(ExamPeriodicity.YEARLY);
    }

    @ParameterizedTest(name = "periodicidade {0} soma {1} meses")
    @DisplayName("o vencimento usa a periodicidade configurada no perfil de saude")
    @CsvSource({"QUARTERLY, 3", "SEMESTERLY, 6", "YEARLY, 12"})
    void shouldAddConfiguredPeriodicityToLastExam(final String periodicity, final int months) {
        givenProfile(periodicity);
        final LocalDate collectedAt = LocalDate.of(2026, 8, 15);
        givenLastExamCollectedAt(collectedAt);

        final ExamGoalDTO goal = service.goalOf(USER_ID);

        assertThat(goal.dueDate()).isEqualTo(collectedAt.plusMonths(months));
        assertThat(goal.periodicity()).isEqualTo(ExamPeriodicity.valueOf(periodicity));
    }

    @ParameterizedTest(name = "periodicidade \"{0}\" no banco cai no padrao YEARLY")
    @DisplayName("periodicidade ausente ou desconhecida usa o padrao do DDL")
    @NullSource
    @ValueSource(strings = {"", "MENSAL", "   "})
    void shouldFallBackToYearlyOnUnknownPeriodicity(final String stored) {
        givenProfile(stored);
        final LocalDate collectedAt = LocalDate.of(2026, 8, 15);
        givenLastExamCollectedAt(collectedAt);

        final ExamGoalDTO goal = service.goalOf(USER_ID);

        assertThat(goal.periodicity()).isEqualTo(ExamPeriodicity.YEARLY);
        assertThat(goal.dueDate()).isEqualTo(collectedAt.plusMonths(12));
    }

    @Test
    @DisplayName("periodicidade gravada em caixa diferente e reconhecida")
    void shouldParsePeriodicityIgnoringCase() {
        givenProfile("semesterly");
        givenLastExamCollectedAt(LocalDate.of(2026, 8, 15));

        assertThat(service.goalOf(USER_ID).periodicity()).isEqualTo(ExamPeriodicity.SEMESTERLY);
    }

    @Test
    @DisplayName("cidadao sem perfil de saude: 404, pois nao ha periodicidade para calcular a meta")
    void shouldFailWhenCitizenHasNoHealthProfile() {
        willThrow(new NotFoundException("Perfil de saúde não encontrado para o usuário: 1"))
                .given(healthProfileService).getByUserId(USER_ID);

        assertThatThrownBy(() -> service.goalOf(USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Perfil de saúde");
    }

    @Test
    @DisplayName("usuario inexistente falha antes de qualquer consulta de perfil ou exame")
    void shouldFailWhenUserDoesNotExist() {
        willThrow(new NotFoundException("Usuário não encontrado para o id: 99"))
                .given(userServiceHelper).getUserByIdOrFail(99L);

        assertThatThrownBy(() -> service.goalOf(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");

        then(healthProfileService).shouldHaveNoInteractions();
        then(examRepository).shouldHaveNoInteractions();
    }

    private void givenProfile(final String periodicity) {
        final HealthProfile profile = new HealthProfile();
        profile.setExamPeriodicity(periodicity);
        given(healthProfileService.getByUserId(USER_ID)).willReturn(profile);
    }

    private void givenLastExamCollectedAt(final LocalDate collectedAt) {
        final Exam exam = new Exam();
        exam.setCollectedAt(OffsetDateTime.of(collectedAt.atTime(8, 30), ZoneOffset.UTC));
        given(examRepository.findFirstByUserIdOrderByCollectedAtDesc(USER_ID)).willReturn(Optional.of(exam));
    }

}
