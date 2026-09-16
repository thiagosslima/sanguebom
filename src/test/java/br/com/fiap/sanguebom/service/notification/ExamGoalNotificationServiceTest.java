package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.config.NotificationProperties;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.ExamGoalStatus;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.model.notification.ExamGoalScanResultDTO;
import br.com.fiap.sanguebom.model.userexam.ExamGoalDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.service.ExamGoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fiap.sanguebom.service.MessageService;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * CF-347 / CF-395 / CF-396 / CF-397 - varredura diaria dos cidadaos ativos, criacao dos avisos de
 * vencimento proximo e de meta vencida, sem repetir aviso ja criado no mesmo ciclo.
 */
@ExtendWith(MockitoExtension.class)
class ExamGoalNotificationServiceTest {

    private static final LocalDate DUE_DATE = LocalDate.of(2027, 2, 15);
    private static final int PAGE_SIZE = 2;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ExamGoalService examGoalService;

    @Mock
    private NotificationDispatcher dispatcher;

    private ExamGoalNotificationService service;

    @BeforeEach
    void setUp() {
        final NotificationProperties properties = new NotificationProperties(
                new NotificationProperties.Sse(60_000L, 25_000L),
                new NotificationProperties.ExamGoal(PAGE_SIZE));
        service = new ExamGoalNotificationService(appUserRepository, examGoalService, dispatcher,
                new MessageService(realMessageSource()), properties);
    }

    @Test
    @DisplayName("meta vencendo em 30 dias gera o aviso de vencimento proximo com a data do ciclo")
    void shouldCreateDueSoonNotification() {
        givenActiveUsers(user(1L));
        givenGoal(1L, ExamGoalStatus.DUE_SOON, 30L);
        givenDispatcherCreates();

        final ExamGoalScanResultDTO result = service.scanActiveUsers();

        then(dispatcher).should().dispatch(any(AppUser.class),
                eq(NotificationType.EXAM_GOAL_DUE_SOON),
                eq("Seu próximo exame está próximo do prazo"),
                eq("Faltam 30 dia(s) para o prazo da sua meta de exames, que vence em 15/02/2027. "
                        + "Agende seu próximo exame em uma unidade de saúde."),
                eq("2027-02-15"));
        assertThat(result).isEqualTo(new ExamGoalScanResultDTO(1, 1, 0, 0));
    }

    @Test
    @DisplayName("meta vencida gera o aviso de meta vencida com a data do ciclo")
    void shouldCreateOverdueNotification() {
        givenActiveUsers(user(1L));
        givenGoal(1L, ExamGoalStatus.OVERDUE, -5L);
        givenDispatcherCreates();

        final ExamGoalScanResultDTO result = service.scanActiveUsers();

        then(dispatcher).should().dispatch(any(AppUser.class),
                eq(NotificationType.EXAM_GOAL_OVERDUE),
                eq("Sua meta de exames está vencida"),
                eq("Sua meta de exames venceu em 15/02/2027. Procure uma unidade de saúde para "
                        + "realizar o próximo exame."),
                eq("2027-02-15"));
        assertThat(result).isEqualTo(new ExamGoalScanResultDTO(1, 0, 1, 0));
    }

    @ParameterizedTest(name = "situacao {0} nao gera aviso")
    @DisplayName("meta em dia ou cidadao sem historico nao geram notificacao")
    @EnumSource(value = ExamGoalStatus.class, names = {"UP_TO_DATE", "NO_HISTORY"})
    void shouldNotNotifyWhenGoalIsFine(final ExamGoalStatus status) {
        givenActiveUsers(user(1L));
        givenGoal(1L, status, 200L);

        final ExamGoalScanResultDTO result = service.scanActiveUsers();

        then(dispatcher).shouldHaveNoInteractions();
        assertThat(result).isEqualTo(new ExamGoalScanResultDTO(1, 0, 0, 0));
    }

    @Test
    @DisplayName("CF-397: rodar a rotina duas vezes no mesmo ciclo nao duplica o aviso")
    void shouldNotDuplicateNotificationWhenScanRunsTwiceInSameCycle() {
        givenActiveUsers(user(1L));
        givenGoal(1L, ExamGoalStatus.DUE_SOON, 30L);
        given(dispatcher.dispatch(any(AppUser.class), any(NotificationType.class), anyString(),
                anyString(), anyString()))
                .willReturn(Optional.of(new Notification()))
                .willReturn(Optional.empty());

        assertThat(service.scanActiveUsers().dueSoon()).isEqualTo(1);
        assertThat(service.scanActiveUsers().dueSoon()).isZero();
    }

    @Test
    @DisplayName("cidadao sem perfil de saude e ignorado e a varredura continua nos demais")
    void shouldSkipCitizenWithoutHealthProfileAndKeepScanning() {
        givenActiveUsers(user(1L), user(2L));
        willThrow(new NotFoundException("Perfil de saúde não encontrado para o usuário: 1"))
                .given(examGoalService).goalOf(1L);
        givenGoal(2L, ExamGoalStatus.OVERDUE, -1L);
        givenDispatcherCreates();

        final ExamGoalScanResultDTO result = service.scanActiveUsers();

        assertThat(result).isEqualTo(new ExamGoalScanResultDTO(2, 0, 1, 1));
    }

    @Test
    @DisplayName("CF-395: a varredura percorre todas as paginas de cidadaos ativos")
    void shouldScanEveryPageOfActiveUsers() {
        final Pageable firstPage = PageRequest.of(0, PAGE_SIZE, Sort.by("id"));
        final Pageable secondPage = PageRequest.of(1, PAGE_SIZE, Sort.by("id"));
        given(appUserRepository.findByStatusIgnoreCase("ACTIVE", firstPage))
                .willReturn(new PageImpl<>(List.of(user(1L), user(2L)), firstPage, 3));
        given(appUserRepository.findByStatusIgnoreCase("ACTIVE", secondPage))
                .willReturn(new PageImpl<>(List.of(user(3L)), secondPage, 3));
        givenGoal(1L, ExamGoalStatus.UP_TO_DATE, 100L);
        givenGoal(2L, ExamGoalStatus.DUE_SOON, 10L);
        givenGoal(3L, ExamGoalStatus.OVERDUE, -10L);
        givenDispatcherCreates();

        final ExamGoalScanResultDTO result = service.scanActiveUsers();

        assertThat(result).isEqualTo(new ExamGoalScanResultDTO(3, 1, 1, 0));
        then(appUserRepository).should().findByStatusIgnoreCase("ACTIVE", firstPage);
        then(appUserRepository).should().findByStatusIgnoreCase("ACTIVE", secondPage);
    }

    @Test
    @DisplayName("so cidadaos ativos entram na varredura")
    void shouldOnlyScanActiveCitizens() {
        givenActiveUsers();

        final ExamGoalScanResultDTO result = service.scanActiveUsers();

        assertThat(result).isEqualTo(new ExamGoalScanResultDTO(0, 0, 0, 0));
        then(examGoalService).shouldHaveNoInteractions();
        then(appUserRepository).should()
                .findByStatusIgnoreCase(eq("ACTIVE"), any(Pageable.class));
        then(appUserRepository).shouldHaveNoMoreInteractions();
    }

    private void givenActiveUsers(final AppUser... users) {
        final Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("id"));
        final Page<AppUser> page = new PageImpl<>(List.of(users), pageable, users.length);
        given(appUserRepository.findByStatusIgnoreCase("ACTIVE", pageable)).willReturn(page);
    }

    private void givenGoal(final Long userId, final ExamGoalStatus status, final Long daysRemaining) {
        final boolean hasHistory = status != ExamGoalStatus.NO_HISTORY;
        given(examGoalService.goalOf(userId)).willReturn(new ExamGoalDTO(
                hasHistory ? DUE_DATE.minusMonths(6) : null,
                hasHistory ? DUE_DATE : null,
                hasHistory ? daysRemaining : null,
                ExamPeriodicity.SEMESTERLY,
                status));
    }

    private void givenDispatcherCreates() {
        given(dispatcher.dispatch(any(AppUser.class), any(NotificationType.class), anyString(),
                anyString(), anyString())).willReturn(Optional.of(new Notification()));
    }

    private static AppUser user(final Long id) {
        final AppUser user = new AppUser();
        user.setId(id);
        user.setStatus("ACTIVE");
        return user;
    }

    private static ResourceBundleMessageSource realMessageSource() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
