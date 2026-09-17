package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.notification.NotificationEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationDeliveryListener {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDeliveryListener.class);

    private final NotificationStreamService streamService;
    private final NotificationDeliveryService deliveryService;

    public NotificationDeliveryListener(final NotificationStreamService streamService,
            final NotificationDeliveryService deliveryService) {
        this.streamService = streamService;
        this.deliveryService = deliveryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(final NotificationCreatedEvent event) {
        final Notification notification = deliveryService.getOrNull(event.notificationId());
        if (notification == null) {
            LOG.warn("Notificacao {} nao encontrada para entrega", event.notificationId());
            return;
        }

        if (streamService.push(event.userId(), NotificationEventDTO.of(notification))) {
            deliveryService.markSent(notification.getId());
        }
    }
}
