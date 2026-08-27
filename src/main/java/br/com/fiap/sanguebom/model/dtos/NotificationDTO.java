package br.com.fiap.sanguebom.model.dtos;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class NotificationDTO {

    private Long id;

    @Size(max = 30)
    private String type;

    @Size(max = 200)
    private String title;

    private String message;

    private OffsetDateTime scheduledAt;

    private OffsetDateTime sentAt;

    @Size(max = 30)
    private String status;

    private OffsetDateTime createdAt;

    private Long user;

}
