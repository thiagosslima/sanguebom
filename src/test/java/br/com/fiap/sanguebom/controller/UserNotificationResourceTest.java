package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import br.com.fiap.sanguebom.service.UserServiceHelper;
import br.com.fiap.sanguebom.service.notification.NotificationQueryService;
import br.com.fiap.sanguebom.service.notification.NotificationStreamService;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.servlet.ServletErrorHandlingConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CF-347 / CF-348 - rotas do cidadao: historico gravado no banco e stream SSE ao vivo.
 */
@WebMvcTest(UserNotificationResource.class)
@Import({NotificationQueryService.class, UserServiceHelper.class})
@ImportAutoConfiguration(ServletErrorHandlingConfiguration.class)
class UserNotificationResourceTest {

    private static final long USER_ID = 1L;
    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.of(2026, 9, 7, 8, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private NotificationStreamService notificationStreamService;

    @Test
    @DisplayName("GET notifications devolve o historico do cidadao, entregue ou nao")
    void shouldReturnNotificationHistory() throws Exception {
        givenUserExists();
        final Pageable pageable = PageRequest.of(0, 10);
        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID, pageable))
                .willReturn(new PageImpl<>(List.of(
                        notification(2L, NotificationType.EXAM_GOAL_DUE_SOON, NotificationStatus.PENDING),
                        notification(1L, NotificationType.EXAM_RESULT_AVAILABLE, NotificationStatus.SENT)),
                        pageable, 2));

        mockMvc.perform(get("/api/users/{userId}/notifications", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].type").value("EXAM_GOAL_DUE_SOON"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].sentAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].referenceKey").value("2027-02-15"))
                .andExpect(jsonPath("$.content[1].type").value("EXAM_RESULT_AVAILABLE"))
                .andExpect(jsonPath("$.content[1].status").value("SENT"));
    }

    @Test
    @DisplayName("GET notifications de cidadao inexistente devolve 404")
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        given(appUserRepository.findById(anyLong())).willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/notifications", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET notifications valida o tamanho da pagina")
    void shouldRejectInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/notifications", USER_ID).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET stream abre a conexao SSE do cidadao")
    void shouldOpenSseStream() throws Exception {
        given(notificationStreamService.subscribe(USER_ID)).willReturn(new SseEmitter(1_000L));

        mockMvc.perform(get("/api/users/{userId}/notifications/stream", USER_ID))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("GET stream de cidadao inexistente devolve 404 e nao abre conexao")
    void shouldReturnNotFoundWhenStreamingUnknownUser() throws Exception {
        willThrow(new NotFoundException("Usuário não encontrado para o id: 99"))
                .given(notificationStreamService).subscribe(eq(99L));

        mockMvc.perform(get("/api/users/{userId}/notifications/stream", 99L))
                .andExpect(status().isNotFound());
    }

    private void givenUserExists() {
        final AppUser user = new AppUser();
        user.setId(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user));
    }

    private static Notification notification(final Long id, final NotificationType type,
            final NotificationStatus status) {
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setType(type);
        notification.setTitle("titulo");
        notification.setMessage("mensagem");
        notification.setStatus(status);
        notification.setReferenceKey(type == NotificationType.EXAM_RESULT_AVAILABLE
                ? "EXAM:10001" : "2027-02-15");
        notification.setCreatedAt(CREATED_AT);
        notification.setSentAt(status == NotificationStatus.SENT ? CREATED_AT : null);
        return notification;
    }
}
