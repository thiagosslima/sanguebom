package br.com.fiap.sanguebom.model.notification;

import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Notificacao do historico do cidadao")
public record NotificationSummaryDTO(

        Long id,

        NotificationType type,

        String title,

        String message,

        @Schema(description = "PENDING enquanto nao foi entregue pelo stream, SENT depois da entrega")
        NotificationStatus status,

        String referenceKey,

        OffsetDateTime createdAt,

        @Schema(description = "Momento da entrega pelo stream; nulo enquanto a notificacao esta pendente")
        OffsetDateTime sentAt

) {

    public static NotificationSummaryDTO of(final Notification notification) {
        return new NotificationSummaryDTO(notification.getId(), notification.getType(),
                notification.getTitle(), notification.getMessage(), notification.getStatus(),
                notification.getReferenceKey(), notification.getCreatedAt(), notification.getSentAt());
    }
}
