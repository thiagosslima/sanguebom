package br.com.fiap.sanguebom.model.notification;

import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Notificacao entregue ao cidadao pelo stream SSE")
public record NotificationEventDTO(

        Long id,

        NotificationType type,

        String title,

        String message,

        @Schema(description = "Ciclo que originou o aviso: EXAM:{id} ou a data de vencimento da meta")
        String referenceKey,

        OffsetDateTime createdAt

) {

    public static NotificationEventDTO of(final Notification notification) {
        return new NotificationEventDTO(notification.getId(), notification.getType(),
                notification.getTitle(), notification.getMessage(), notification.getReferenceKey(),
                notification.getCreatedAt());
    }
}
