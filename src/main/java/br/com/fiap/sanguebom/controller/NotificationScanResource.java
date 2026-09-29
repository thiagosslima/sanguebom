package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.notification.ExamGoalScanResultDTO;
import br.com.fiap.sanguebom.service.notification.ExamGoalNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Rotina de notificações", description = "Disparo manual da varredura da meta de exames")
@RequestMapping(value = "/api/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationScanResource {

    private final ExamGoalNotificationService examGoalNotificationService;

    public NotificationScanResource(final ExamGoalNotificationService examGoalNotificationService) {
        this.examGoalNotificationService = examGoalNotificationService;
    }

    @PostMapping("/exam-goal-scan")
    @Operation(summary = "Executa a varredura da meta de exames",
            description = "Mesma rotina do agendamento diário, sob demanda: percorre os cidadãos "
                    + "ativos e cria os avisos de vencimento próximo e de meta vencida, sem repetir "
                    + "aviso já criado no mesmo ciclo.")
    public ResponseEntity<ExamGoalScanResultDTO> runExamGoalScan() {
        return ResponseEntity.ok(examGoalNotificationService.scanActiveUsers());
    }
}
