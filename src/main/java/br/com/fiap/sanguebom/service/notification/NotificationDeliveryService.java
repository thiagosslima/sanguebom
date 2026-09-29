package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class NotificationDeliveryService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationDeliveryService(final NotificationRepository notificationRepository,
            final Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(final Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getStatus() == NotificationStatus.SENT) {
                return;
            }
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now(clock));
            notificationRepository.save(notification);
        });
    }

    @Transactional(readOnly = true)
    public Notification getOrNull(final Long notificationId) {
        return notificationRepository.findById(notificationId).orElse(null);
    }
}
