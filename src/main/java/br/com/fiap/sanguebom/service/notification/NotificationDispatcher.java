package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class NotificationDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public NotificationDispatcher(final NotificationRepository notificationRepository,
            final ApplicationEventPublisher publisher,
            final Clock clock) {
        this.notificationRepository = notificationRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public Optional<Notification> dispatch(final AppUser user, final NotificationType type,
            final String title, final String message, final String referenceKey) {
        if (notificationRepository.existsByUserIdAndTypeAndReferenceKey(user.getId(), type, referenceKey)) {
            LOG.debug("Aviso {} ja criado para o usuario {} no ciclo {}", type, user.getId(), referenceKey);
            return Optional.empty();
        }

        final Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceKey(referenceKey);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setScheduledAt(OffsetDateTime.now(clock));

        final Notification saved = notificationRepository.save(notification);
        publisher.publishEvent(new NotificationCreatedEvent(saved.getId(), user.getId()));
        return Optional.of(saved);
    }
}
