package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import br.com.fiap.sanguebom.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CF-349 - consulta de notificacoes do usuario, na rota /api/notifications.
 *
 * <p>Teste de ponta a ponta dentro da aplicacao: rota -> controller -> service, incluindo o
 * tratamento de excecoes do {@code GlobalExceptionHandler}. So os repositorios sao dublados.</p>
 */
@WebMvcTest(NotificationResource.class)
@Import(NotificationService.class)
class NotificationResourceTest {

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private AppUserRepository appUserRepository;

    // ---------------------------------------------------------------- GET (lista)

    @Test
    @DisplayName("GET /api/notifications sem parametros devolve todas as notificacoes")
    void shouldReturnAllNotifications() throws Exception {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(notification(USER_ID, "SENT"), notification(2L, "PENDING")));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/notifications?userId= devolve apenas as notificacoes do titular")
    void shouldFilterByUserId() throws Exception {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(notification(USER_ID, "SENT"), notification(2L, "SENT")));

        mockMvc.perform(get("/api/notifications").param("userId", String.valueOf(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user").value(USER_ID));
    }

    @Test
    @DisplayName("GET /api/notifications?status= devolve apenas as notificacoes com aquele status")
    void shouldFilterByStatus() throws Exception {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(notification(USER_ID, "SENT"), notification(USER_ID, "PENDING")));

        mockMvc.perform(get("/api/notifications").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/notifications com userId e status combinados aplica os dois filtros")
    void shouldFilterByUserIdAndStatus() throws Exception {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(
                        notification(USER_ID, "SENT"),
                        notification(USER_ID, "PENDING"),
                        notification(2L, "PENDING")));

        mockMvc.perform(get("/api/notifications")
                        .param("userId", String.valueOf(USER_ID))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user").value(USER_ID))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ---------------------------------------------------------------- GET (por id)

    @Test
    @DisplayName("GET /api/notifications/{id} devolve a notificacao quando o id existe")
    void shouldReturnNotificationById() throws Exception {
        given(notificationRepository.findById(NOTIFICATION_ID))
                .willReturn(Optional.of(notification(USER_ID, "SENT")));

        mockMvc.perform(get("/api/notifications/{id}", NOTIFICATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("GET /api/notifications/{id} de id inexistente devolve 404 como ProblemDetail")
    void shouldReturnNotFoundWhenNotificationDoesNotExist() throws Exception {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Notification not found"));
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("POST /api/notifications valido devolve 201 com o id criado")
    void shouldCreateNotification() throws Exception {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            final Notification saved = invocation.getArgument(0);
            saved.setId(NOTIFICATION_ID);
            return saved;
        });

        mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "REMINDER",
                                  "title": "Titulo",
                                  "message": "Mensagem",
                                  "status": "PENDING",
                                  "user": %d
                                }
                                """.formatted(USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(NOTIFICATION_ID));
    }

    @Test
    @DisplayName("POST /api/notifications com campo type maior que 30 caracteres devolve 400")
    void shouldRejectInvalidType() throws Exception {
        final String longType = "T".repeat(31);

        mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "%s",
                                  "title": "Titulo",
                                  "message": "Mensagem",
                                  "status": "PENDING"
                                }
                                """.formatted(longType)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetros inválidos"))
                .andExpect(jsonPath("$.['parâmetros inválidos'][0].field").value("type"));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    // ---------------------------------------------------------------- PUT

    @Test
    @DisplayName("PUT /api/notifications/{id} valido devolve 200 com o id atualizado")
    void shouldUpdateNotification() throws Exception {
        given(notificationRepository.findById(NOTIFICATION_ID))
                .willReturn(Optional.of(notification(USER_ID, "PENDING")));
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));

        mockMvc.perform(put("/api/notifications/{id}", NOTIFICATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "REMINDER",
                                  "title": "Titulo",
                                  "message": "Mensagem",
                                  "status": "SENT",
                                  "user": %d
                                }
                                """.formatted(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(NOTIFICATION_ID));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id} de id inexistente devolve 404 como ProblemDetail")
    void shouldReturnNotFoundWhenUpdatingMissingNotification() throws Exception {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(put("/api/notifications/{id}", 999L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "REMINDER",
                                  "title": "Titulo",
                                  "message": "Mensagem",
                                  "status": "SENT"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Notification not found"));

        then(appUserRepository).shouldHaveNoInteractions();
    }

    // ---------------------------------------------------------------- fixtures

    private static Notification notification(final Long userId, final String status) {
        final Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setType("REMINDER");
        notification.setTitle("Titulo");
        notification.setMessage("Mensagem");
        notification.setStatus(status);
        notification.setUser(appUser(userId));
        return notification;
    }

    private static AppUser appUser(final Long id) {
        final AppUser user = new AppUser();
        user.setId(id);
        return user;
    }

}
