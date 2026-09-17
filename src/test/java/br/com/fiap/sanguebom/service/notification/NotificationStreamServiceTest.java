package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.config.NotificationProperties;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.model.notification.NotificationEventDTO;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import br.com.fiap.sanguebom.service.UserServiceHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * CF-347 / CF-348 - o stream SSE e apenas o canal ao vivo: o que nao for entregue continua no
 * historico como PENDING e e reenviado na proxima conexao do cidadao.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationStreamServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private UserServiceHelper userServiceHelper;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryService deliveryService;

    private TestableStreamService service;

    @BeforeEach
    void setUp() {
        final NotificationProperties properties = new NotificationProperties(
                new NotificationProperties.Sse(60_000L, 25_000L),
                new NotificationProperties.ExamGoal(200));
        service = new TestableStreamService(userServiceHelper, notificationRepository,
                deliveryService, properties);
        givenUserExists(USER_ID);
        givenNoPendingNotifications(USER_ID);
    }

    @Test
    @DisplayName("ao abrir o stream o cidadao recebe o evento connected e a conexao fica registrada")
    void shouldRegisterConnectionAndSendConnectedEvent() {
        final RecordingEmitter emitter = service.nextEmitter();

        service.subscribe(USER_ID);

        assertThat(service.openConnections(USER_ID)).isEqualTo(1);
        assertThat(emitter.rawEvents()).anyMatch(data -> data.contains("event:connected"));
    }

    @Test
    @DisplayName("cidadao inexistente nao abre stream")
    void shouldFailForUnknownUser() {
        willThrow(new NotFoundException("Usuário não encontrado para o id: 99"))
                .given(userServiceHelper).getUserByIdOrFail(99L);

        assertThatThrownBy(() -> service.subscribe(99L)).isInstanceOf(NotFoundException.class);
        assertThat(service.openConnections(99L)).isZero();
    }

    @Test
    @DisplayName("notificacoes pendentes sao reenviadas na conexao e passam a SENT")
    void shouldReplayPendingNotificationsOnSubscribe() {
        final Notification pending = notification(70L, NotificationType.EXAM_RESULT_AVAILABLE);
        given(notificationRepository.findByUserIdAndStatusOrderByCreatedAtAsc(USER_ID,
                NotificationStatus.PENDING)).willReturn(List.of(pending));
        final RecordingEmitter emitter = service.nextEmitter();

        service.subscribe(USER_ID);

        assertThat(emitter.payloads()).extracting(NotificationEventDTO::id).containsExactly(70L);
        then(deliveryService).should().markSent(70L);
    }

    @Test
    @DisplayName("o push alcanca todas as conexoes abertas do mesmo cidadao")
    void shouldPushToEveryConnectionOfUser() {
        final RecordingEmitter first = service.nextEmitter();
        service.subscribe(USER_ID);
        final RecordingEmitter second = service.nextEmitter();
        service.subscribe(USER_ID);

        final boolean delivered = service.push(USER_ID, event(80L));

        assertThat(delivered).isTrue();
        assertThat(first.payloads()).extracting(NotificationEventDTO::id).containsExactly(80L);
        assertThat(second.payloads()).extracting(NotificationEventDTO::id).containsExactly(80L);
    }

    @Test
    @DisplayName("o push nao vaza notificacao de um cidadao para o stream de outro")
    void shouldNotPushToAnotherUser() {
        final RecordingEmitter emitter = service.nextEmitter();
        service.subscribe(USER_ID);

        assertThat(service.push(OTHER_USER_ID, event(81L))).isFalse();
        assertThat(emitter.payloads()).isEmpty();
    }

    @Test
    @DisplayName("sem ninguem conectado o push falha e a notificacao continua pendente")
    void shouldReportNotDeliveredWhenNobodyIsConnected() {
        assertThat(service.push(USER_ID, event(82L))).isFalse();
    }

    @Test
    @DisplayName("conexao quebrada e descartada sem afetar as demais do cidadao")
    void shouldDropBrokenConnectionAndKeepDeliveringToOthers() {
        final RecordingEmitter broken = service.nextFailingEmitter();
        service.subscribe(USER_ID);
        final RecordingEmitter healthy = service.nextEmitter();
        service.subscribe(USER_ID);

        final boolean delivered = service.push(USER_ID, event(83L));

        assertThat(delivered).isTrue();
        assertThat(healthy.payloads()).extracting(NotificationEventDTO::id).containsExactly(83L);
        assertThat(service.openConnections(USER_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("o heartbeat mantem as conexoes vivas e limpa as que morreram")
    void shouldPingOpenConnectionsAndRemoveDeadOnes() {
        final RecordingEmitter healthy = service.nextEmitter();
        service.subscribe(USER_ID);

        service.heartbeat();
        assertThat(healthy.rawEvents()).anyMatch(data -> data.contains("event:ping"));

        healthy.failing = true;
        service.heartbeat();
        assertThat(service.openConnections(USER_ID)).isZero();
    }

    @Test
    @DisplayName("quando a conexao cai durante o reenvio, o restante segue pendente para a proxima")
    void shouldStopReplayWhenConnectionDiesAndKeepRemainingPending() {
        given(notificationRepository.findByUserIdAndStatusOrderByCreatedAtAsc(USER_ID,
                NotificationStatus.PENDING))
                .willReturn(List.of(notification(90L, NotificationType.EXAM_GOAL_DUE_SOON),
                        notification(91L, NotificationType.EXAM_GOAL_OVERDUE)));
        service.nextFailingEmitterAfterConnected();

        service.subscribe(USER_ID);

        then(deliveryService).shouldHaveNoInteractions();
        assertThat(service.openConnections(USER_ID)).isZero();
    }

    private void givenUserExists(final Long userId) {
        final AppUser user = new AppUser();
        user.setId(userId);
        given(userServiceHelper.getUserByIdOrFail(userId)).willReturn(user);
    }

    private void givenNoPendingNotifications(final Long userId) {
        given(notificationRepository.findByUserIdAndStatusOrderByCreatedAtAsc(userId,
                NotificationStatus.PENDING)).willReturn(List.of());
    }

    private static NotificationEventDTO event(final Long id) {
        return new NotificationEventDTO(id, NotificationType.EXAM_RESULT_AVAILABLE, "titulo",
                "mensagem", "EXAM:1", OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static Notification notification(final Long id, final NotificationType type) {
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setType(type);
        notification.setTitle("titulo");
        notification.setMessage("mensagem");
        notification.setStatus(NotificationStatus.PENDING);
        return notification;
    }

    /**
     * Deixa o teste inspecionar o que foi enviado em cada conexao, sem contexto assincrono real.
     */
    private static final class TestableStreamService extends NotificationStreamService {

        private final Deque<RecordingEmitter> prepared = new ArrayDeque<>();

        private TestableStreamService(final UserServiceHelper userServiceHelper,
                final NotificationRepository notificationRepository,
                final NotificationDeliveryService deliveryService,
                final NotificationProperties properties) {
            super(userServiceHelper, notificationRepository, deliveryService, properties);
        }

        @Override
        protected SseEmitter createEmitter(final long timeoutMs) {
            return prepared.isEmpty() ? new RecordingEmitter() : prepared.poll();
        }

        private RecordingEmitter nextEmitter() {
            final RecordingEmitter emitter = new RecordingEmitter();
            prepared.add(emitter);
            return emitter;
        }

        private RecordingEmitter nextFailingEmitter() {
            final RecordingEmitter emitter = nextEmitter();
            emitter.failing = true;
            return emitter;
        }

        private RecordingEmitter nextFailingEmitterAfterConnected() {
            final RecordingEmitter emitter = nextEmitter();
            emitter.failAfterFirstEvent = true;
            return emitter;
        }
    }

    private static final class RecordingEmitter extends SseEmitter {

        private final List<Object> sent = new ArrayList<>();
        private boolean failing;
        private boolean failAfterFirstEvent;

        @Override
        public void send(final SseEventBuilder builder) throws IOException {
            if (failing) {
                throw new IOException("conexao encerrada pelo cliente");
            }
            builder.build().forEach(data -> sent.add(data.getData()));
            if (failAfterFirstEvent) {
                failing = true;
            }
        }

        @Override
        public void send(final Object object, final MediaType mediaType) {
            sent.add(object);
        }

        private List<String> rawEvents() {
            return sent.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }

        private List<NotificationEventDTO> payloads() {
            return sent.stream()
                    .filter(NotificationEventDTO.class::isInstance)
                    .map(NotificationEventDTO.class::cast)
                    .toList();
        }
    }
}
