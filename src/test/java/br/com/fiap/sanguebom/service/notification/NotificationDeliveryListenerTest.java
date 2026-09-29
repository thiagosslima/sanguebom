package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.model.notification.NotificationEventDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * A entrega e um atributo da linha do historico, nunca um substituto dela: falhar o envio
 * mantem a notificacao salva como PENDING para o proximo acesso do cidadao.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDeliveryListenerTest {

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 42L;

    @Mock
    private NotificationStreamService streamService;

    @Mock
    private NotificationDeliveryService deliveryService;

    @InjectMocks
    private NotificationDeliveryListener listener;

    @Test
    @DisplayName("entregue pelo stream, a notificacao e marcada como SENT")
    void shouldMarkSentWhenDelivered() {
        given(deliveryService.getOrNull(NOTIFICATION_ID)).willReturn(notification());
        given(streamService.push(eq(USER_ID), any(NotificationEventDTO.class))).willReturn(true);

        listener.onNotificationCreated(new NotificationCreatedEvent(NOTIFICATION_ID, USER_ID));

        then(deliveryService).should().markSent(NOTIFICATION_ID);
    }

    @Test
    @DisplayName("sem ninguem conectado a notificacao continua salva como PENDING")
    void shouldKeepPendingWhenNobodyIsConnected() {
        given(deliveryService.getOrNull(NOTIFICATION_ID)).willReturn(notification());
        given(streamService.push(eq(USER_ID), any(NotificationEventDTO.class))).willReturn(false);

        listener.onNotificationCreated(new NotificationCreatedEvent(NOTIFICATION_ID, USER_ID));

        then(deliveryService).should(Mockito.never()).markSent(NOTIFICATION_ID);
    }

    @Test
    @DisplayName("o payload enviado carrega os dados gravados da notificacao")
    void shouldSendStoredNotificationAsPayload() {
        given(deliveryService.getOrNull(NOTIFICATION_ID)).willReturn(notification());
        given(streamService.push(eq(USER_ID), any(NotificationEventDTO.class))).willReturn(true);
        final ArgumentCaptor<NotificationEventDTO> captor =
                ArgumentCaptor.forClass(NotificationEventDTO.class);

        listener.onNotificationCreated(new NotificationCreatedEvent(NOTIFICATION_ID, USER_ID));

        then(streamService).should().push(eq(USER_ID), captor.capture());
        final NotificationEventDTO payload = captor.getValue();
        assertThat(payload.id()).isEqualTo(NOTIFICATION_ID);
        assertThat(payload.type()).isEqualTo(NotificationType.EXAM_RESULT_AVAILABLE);
        assertThat(payload.title()).isEqualTo("Resultado de exame disponível");
        assertThat(payload.referenceKey()).isEqualTo("EXAM:10001");
    }

    @Test
    @DisplayName("notificacao inexistente nao quebra a entrega")
    void shouldIgnoreMissingNotification() {
        given(deliveryService.getOrNull(NOTIFICATION_ID)).willReturn(null);

        listener.onNotificationCreated(new NotificationCreatedEvent(NOTIFICATION_ID, USER_ID));

        then(streamService).shouldHaveNoInteractions();
    }

    private static Notification notification() {
        final Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setType(NotificationType.EXAM_RESULT_AVAILABLE);
        notification.setTitle("Resultado de exame disponível");
        notification.setMessage("mensagem");
        notification.setReferenceKey("EXAM:10001");
        notification.setStatus(NotificationStatus.PENDING);
        return notification;
    }
}
