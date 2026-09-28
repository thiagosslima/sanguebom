package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.notification.NotificationSummaryDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.notification.NotificationQueryService;
import br.com.fiap.sanguebom.service.notification.NotificationStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Validated
@Tag(name = "Notificações do usuário",
        description = "Histórico de avisos do cidadão e stream em tempo real (SSE)")
@RequestMapping("/api/users/{userId}/notifications")
public class UserNotificationResource {

    private final NotificationStreamService notificationStreamService;
    private final NotificationQueryService notificationQueryService;

    public UserNotificationResource(final NotificationStreamService notificationStreamService,
            final NotificationQueryService notificationQueryService) {
        this.notificationStreamService = notificationStreamService;
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Histórico de notificações",
            description = "Avisos do cidadão, paginados do mais recente para o mais antigo. Toda "
                    + "notificação fica gravada, entregue ou não: status PENDING enquanto o stream "
                    + "não a entregou, SENT depois da entrega.")
    public ResponseEntity<PageResponse<NotificationSummaryDTO>> getNotifications(
            @PathVariable(name = "userId") final Long userId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) final int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) final int size) {
        return ResponseEntity.ok(notificationQueryService.history(userId, page, size));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream de notificações (SSE)",
            description = "Abre a conexão de eventos do cidadão. Envia o evento connected na "
                    + "abertura, reenvia as notificações pendentes, entrega os avisos novos no "
                    + "evento notification e mantém a conexão viva com o evento ping.")
    public SseEmitter streamNotifications(@PathVariable(name = "userId") final Long userId) {
        return notificationStreamService.subscribe(userId);
    }
}
