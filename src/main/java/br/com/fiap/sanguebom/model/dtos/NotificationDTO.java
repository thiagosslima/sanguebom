package br.com.fiap.sanguebom.model.dtos;

import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class NotificationDTO {

    private Long id;

    private NotificationType type;

    @Size(max = 200)
    private String title;

    private String message;

    private OffsetDateTime scheduledAt;

    private OffsetDateTime sentAt;

    private NotificationStatus status;

    @Size(max = 120)
    private String referenceKey;

    private OffsetDateTime createdAt;

    private Long user;

}
