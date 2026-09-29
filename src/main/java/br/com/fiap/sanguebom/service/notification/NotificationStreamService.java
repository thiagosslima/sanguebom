package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.config.NotificationProperties;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.notification.NotificationEventDTO;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import br.com.fiap.sanguebom.service.UserServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationStreamService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationStreamService.class);

    static final String EVENT_CONNECTED = "connected";
    static final String EVENT_NOTIFICATION = "notification";
    static final String EVENT_PING = "ping";

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final UserServiceHelper userServiceHelper;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryService deliveryService;
    private final NotificationProperties properties;

    public NotificationStreamService(final UserServiceHelper userServiceHelper,
            final NotificationRepository notificationRepository,
            final NotificationDeliveryService deliveryService,
            final NotificationProperties properties) {
        this.userServiceHelper = userServiceHelper;
        this.notificationRepository = notificationRepository;
        this.deliveryService = deliveryService;
        this.properties = properties;
    }

    public SseEmitter subscribe(final Long userId) {
        userServiceHelper.getUserByIdOrFail(userId);

        final SseEmitter emitter = createEmitter(properties.sse().timeoutMs());
        register(userId, emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(throwable -> remove(userId, emitter));

        if (!send(emitter, SseEmitter.event().name(EVENT_CONNECTED).data(Map.of("userId", userId)))) {
            remove(userId, emitter);
            return emitter;
        }

        replayPending(userId, emitter);
        return emitter;
    }

    public boolean push(final Long userId, final NotificationEventDTO payload) {
        final List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            LOG.debug("Nenhuma conexao aberta para o usuario {}; notificacao segue pendente", userId);
            return false;
        }

        boolean delivered = false;
        for (final SseEmitter emitter : new ArrayList<>(userEmitters)) {
            if (send(emitter, SseEmitter.event()
                    .name(EVENT_NOTIFICATION)
                    .data(payload, MediaType.APPLICATION_JSON))) {
                delivered = true;
            } else {
                remove(userId, emitter);
            }
        }
        return delivered;
    }

    @Scheduled(fixedRateString = "${sanguebom.notifications.sse.heartbeat-ms:25000}")
    public void heartbeat() {
        emitters.forEach((userId, userEmitters) -> {
            for (final SseEmitter emitter : new ArrayList<>(userEmitters)) {
                if (!send(emitter, SseEmitter.event().name(EVENT_PING).data("keep-alive"))) {
                    remove(userId, emitter);
                }
            }
        });
    }

    protected SseEmitter createEmitter(final long timeoutMs) {
        return new SseEmitter(timeoutMs);
    }

    int openConnections(final Long userId) {
        final List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters == null ? 0 : userEmitters.size();
    }

    private void replayPending(final Long userId, final SseEmitter emitter) {
        final List<Notification> pending = notificationRepository
                .findByUserIdAndStatusOrderByCreatedAtAsc(userId, NotificationStatus.PENDING);

        for (final Notification notification : pending) {
            if (!send(emitter, SseEmitter.event()
                    .name(EVENT_NOTIFICATION)
                    .data(NotificationEventDTO.of(notification), MediaType.APPLICATION_JSON))) {
                remove(userId, emitter);
                return;
            }
            deliveryService.markSent(notification.getId());
        }
    }

    private void register(final Long userId, final SseEmitter emitter) {
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    private void remove(final Long userId, final SseEmitter emitter) {
        emitters.computeIfPresent(userId, (key, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }

    private boolean send(final SseEmitter emitter, final SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
            return true;
        } catch (final IOException | IllegalStateException exception) {
            LOG.debug("Conexao SSE encerrada durante o envio: {}", exception.getMessage());
            return false;
        }
    }
}
