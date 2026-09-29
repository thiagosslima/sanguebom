package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * CF-397 - o aviso e sempre gravado no historico antes de qualquer entrega e nunca se repete
 * dentro do mesmo ciclo.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final Long USER_ID = 1L;
    private static final String CYCLE = "2027-02-15";
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 9, 7, 8, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    private NotificationDispatcher dispatcher;
    private AppUser user;

    @BeforeEach
    void setUp() {
        final Clock clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);
        dispatcher = new NotificationDispatcher(notificationRepository, publisher, clock);
        user = new AppUser();
        user.setId(USER_ID);
    }

    @Test
    @DisplayName("grava a notificacao como PENDING e publica o evento de entrega")
    void shouldPersistNotificationAndPublishEvent() {
        givenNoNotificationInCycle();
        givenRepositoryAssignsId(500L);

        final Optional<Notification> created = dispatcher.dispatch(user,
                NotificationType.EXAM_GOAL_DUE_SOON, "titulo", "mensagem", CYCLE);

        assertThat(created).isPresent();
        final Notification notification = created.orElseThrow();
        assertThat(notification.getUser()).isSameAs(user);
        assertThat(notification.getType()).isEqualTo(NotificationType.EXAM_GOAL_DUE_SOON);
        assertThat(notification.getTitle()).isEqualTo("titulo");
        assertThat(notification.getMessage()).isEqualTo("mensagem");
        assertThat(notification.getReferenceKey()).isEqualTo(CYCLE);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getScheduledAt()).isEqualTo(NOW);
        assertThat(notification.getSentAt()).isNull();

        then(publisher).should().publishEvent(new NotificationCreatedEvent(500L, USER_ID));
    }

    @Test
    @DisplayName("o mesmo aviso no mesmo ciclo nao gera segunda notificacao nem novo evento")
    void shouldNotDuplicateNotificationInSameCycle() {
        given(notificationRepository.existsByUserIdAndTypeAndReferenceKey(USER_ID,
                NotificationType.EXAM_GOAL_DUE_SOON, CYCLE)).willReturn(true);

        final Optional<Notification> created = dispatcher.dispatch(user,
                NotificationType.EXAM_GOAL_DUE_SOON, "titulo", "mensagem", CYCLE);

        assertThat(created).isEmpty();
        then(notificationRepository).should(org.mockito.Mockito.never()).save(any());
        then(publisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("tipos diferentes convivem no mesmo ciclo: vencimento proximo e meta vencida")
    void shouldAllowDifferentTypesInSameCycle() {
        given(notificationRepository.existsByUserIdAndTypeAndReferenceKey(USER_ID,
                NotificationType.EXAM_GOAL_DUE_SOON, CYCLE)).willReturn(true);
        given(notificationRepository.existsByUserIdAndTypeAndReferenceKey(USER_ID,
                NotificationType.EXAM_GOAL_OVERDUE, CYCLE)).willReturn(false);
        givenRepositoryAssignsId(501L);

        assertThat(dispatcher.dispatch(user, NotificationType.EXAM_GOAL_DUE_SOON, "t", "m", CYCLE))
                .isEmpty();
        assertThat(dispatcher.dispatch(user, NotificationType.EXAM_GOAL_OVERDUE, "t", "m", CYCLE))
                .isPresent();
    }

    @Test
    @DisplayName("um novo ciclo, com outra data de vencimento, volta a avisar o cidadao")
    void shouldNotifyAgainOnNewCycle() {
        final String nextCycle = LocalDate.parse(CYCLE).plusMonths(6).toString();
        given(notificationRepository.existsByUserIdAndTypeAndReferenceKey(USER_ID,
                NotificationType.EXAM_GOAL_DUE_SOON, nextCycle)).willReturn(false);
        givenRepositoryAssignsId(502L);

        final ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        assertThat(dispatcher.dispatch(user, NotificationType.EXAM_GOAL_DUE_SOON, "t", "m", nextCycle))
                .isPresent();

        then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getReferenceKey()).isEqualTo(nextCycle);
    }

    private void givenNoNotificationInCycle() {
        given(notificationRepository.existsByUserIdAndTypeAndReferenceKey(USER_ID,
                NotificationType.EXAM_GOAL_DUE_SOON, CYCLE)).willReturn(false);
    }

    private void givenRepositoryAssignsId(final Long id) {
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            final Notification notification = invocation.getArgument(0);
            notification.setId(id);
            return notification;
        });
    }
}
