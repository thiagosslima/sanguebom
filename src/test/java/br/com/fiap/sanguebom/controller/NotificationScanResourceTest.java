package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.notification.ExamGoalScanResultDTO;
import br.com.fiap.sanguebom.service.notification.ExamGoalNotificationService;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.servlet.ServletErrorHandlingConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CF-347 - disparo manual da mesma rotina executada pelo agendamento diario.
 */
@WebMvcTest(NotificationScanResource.class)
@ImportAutoConfiguration(ServletErrorHandlingConfiguration.class)
class NotificationScanResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamGoalNotificationService examGoalNotificationService;

    @Test
    @DisplayName("POST exam-goal-scan roda a varredura e devolve os contadores da execucao")
    void shouldRunScanAndReturnCounters() throws Exception {
        given(examGoalNotificationService.scanActiveUsers())
                .willReturn(new ExamGoalScanResultDTO(12, 3, 2, 1));

        mockMvc.perform(post("/api/notifications/exam-goal-scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanned").value(12))
                .andExpect(jsonPath("$.dueSoon").value(3))
                .andExpect(jsonPath("$.overdue").value(2))
                .andExpect(jsonPath("$.skipped").value(1));

        then(examGoalNotificationService).should().scanActiveUsers();
    }
}
