package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.BadRequestException;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.NotificationDTO;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * CF-349 - consulta de notificacoes do usuario.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long NOTIFICATION_ID = 100L;
    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, appUserRepository);
    }

    // ---------------------------------------------------------------- findAll

    @Test
    @DisplayName("sem filtros, findAll devolve todas as notificacoes")
    void shouldReturnAllNotificationsWithoutFilters() {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(notification(USER_ID, NotificationStatus.SENT), notification(OTHER_USER_ID, NotificationStatus.PENDING)));

        final List<NotificationDTO> result = service.findAll(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("filtro por userId devolve apenas as notificacoes do titular")
    void shouldFilterByUserId() {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(notification(USER_ID, NotificationStatus.SENT), notification(OTHER_USER_ID, NotificationStatus.SENT)));

        final List<NotificationDTO> result = service.findAll(USER_ID, null);

        assertThat(result).singleElement()
                .extracting(NotificationDTO::getUser).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("filtro por status devolve apenas as notificacoes com aquele status")
    void shouldFilterByStatus() {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(notification(USER_ID, NotificationStatus.SENT), notification(USER_ID, NotificationStatus.PENDING)));

        final List<NotificationDTO> result = service.findAll(null, NotificationStatus.PENDING);

        assertThat(result).singleElement()
                .extracting(NotificationDTO::getStatus).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("filtro combinado de userId e status so devolve notificacoes que atendem aos dois")
    void shouldFilterByUserIdAndStatus() {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(
                        notification(USER_ID, NotificationStatus.SENT),
                        notification(USER_ID, NotificationStatus.PENDING),
                        notification(OTHER_USER_ID, NotificationStatus.PENDING)));

        final List<NotificationDTO> result = service.findAll(USER_ID, NotificationStatus.PENDING);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getUser()).isEqualTo(USER_ID);
            assertThat(dto.getStatus()).isEqualTo(NotificationStatus.PENDING);
        });
    }

    @Test
    @DisplayName("filtro sem nenhuma notificacao correspondente devolve lista vazia, e nao erro")
    void shouldReturnEmptyListWhenNoMatch() {
        given(notificationRepository.findAll(any(Sort.class)))
                .willReturn(List.of(notification(OTHER_USER_ID, NotificationStatus.SENT)));

        final List<NotificationDTO> result = service.findAll(USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("notificacao sem usuario associado e ignorada ao filtrar por userId, sem lancar excecao")
    void shouldIgnoreNotificationWithoutUserWhenFilteringByUserId() {
        final Notification withoutUser = notification(null, NotificationStatus.SENT);
        withoutUser.setUser(null);
        given(notificationRepository.findAll(any(Sort.class))).willReturn(List.of(withoutUser));

        final List<NotificationDTO> result = service.findAll(USER_ID, null);

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------- get

    @Test
    @DisplayName("get devolve a notificacao quando o id existe")
    void shouldReturnNotificationById() {
        given(notificationRepository.findById(NOTIFICATION_ID))
                .willReturn(Optional.of(notification(USER_ID, NotificationStatus.SENT)));

        final NotificationDTO dto = service.get(NOTIFICATION_ID);

        assertThat(dto.getUser()).isEqualTo(USER_ID);
        assertThat(dto.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("get de id inexistente lanca NotFoundException")
    void shouldFailWhenNotificationDoesNotExist() {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("create mapeia o DTO, associa o usuario e devolve o id gerado")
    void shouldCreateNotification() {
        final AppUser user = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            final Notification saved = invocation.getArgument(0);
            saved.setId(NOTIFICATION_ID);
            return saved;
        });

        final Long id = service.create(notificationDTO(USER_ID, NotificationStatus.PENDING));

        assertThat(id).isEqualTo(NOTIFICATION_ID);
        then(notificationRepository).should().save(any(Notification.class));
    }

    @Test
    @DisplayName("create com usuario inexistente lanca NotFoundException, e nao BadRequestException")
    void shouldNotMaskNotFoundExceptionOnCreate() {
        given(appUserRepository.findById(OTHER_USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(notificationDTO(OTHER_USER_ID, NotificationStatus.PENDING)))
                .isInstanceOf(NotFoundException.class);

        then(notificationRepository).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("falha de persistencia no create vira BadRequestException")
    void shouldWrapDataAccessExceptionOnCreate() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));
        willThrow(new DataIntegrityViolationException("value too long for column"))
                .given(notificationRepository).save(any(Notification.class));

        assertThatThrownBy(() -> service.create(notificationDTO(USER_ID, NotificationStatus.PENDING)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Error ao criar a notificação");
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("update mapeia o DTO na notificacao existente e salva")
    void shouldUpdateNotification() {
        final Notification existing = notification(USER_ID, NotificationStatus.PENDING);
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(existing));
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));

        service.update(NOTIFICATION_ID, notificationDTO(USER_ID, NotificationStatus.SENT));

        assertThat(existing.getStatus()).isEqualTo(NotificationStatus.SENT);
        then(notificationRepository).should().save(existing);
    }

    @Test
    @DisplayName("update de notificacao inexistente lanca NotFoundException")
    void shouldFailUpdateWhenNotificationDoesNotExist() {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, notificationDTO(USER_ID, NotificationStatus.SENT)))
                .isInstanceOf(NotFoundException.class);

        then(appUserRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("update com usuario inexistente lanca NotFoundException, e nao BadRequestException")
    void shouldNotMaskNotFoundExceptionOnUpdate() {
        given(notificationRepository.findById(NOTIFICATION_ID))
                .willReturn(Optional.of(notification(USER_ID, NotificationStatus.PENDING)));
        given(appUserRepository.findById(OTHER_USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(NOTIFICATION_ID, notificationDTO(OTHER_USER_ID, NotificationStatus.SENT)))
                .isInstanceOf(NotFoundException.class);

        then(notificationRepository).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("falha de persistencia no update vira BadRequestException")
    void shouldWrapDataAccessExceptionOnUpdate() {
        given(notificationRepository.findById(NOTIFICATION_ID))
                .willReturn(Optional.of(notification(USER_ID, NotificationStatus.PENDING)));
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));
        willThrow(new DataIntegrityViolationException("value too long for column"))
                .given(notificationRepository).save(any(Notification.class));

        assertThatThrownBy(() -> service.update(NOTIFICATION_ID, notificationDTO(USER_ID, NotificationStatus.SENT)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Error ao atualizar a notificação");
    }

    // ---------------------------------------------------------------- delete

    @Test
    @DisplayName("delete remove a notificacao existente")
    void shouldDeleteNotification() {
        final Notification existing = notification(USER_ID, NotificationStatus.SENT);
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(existing));

        service.delete(NOTIFICATION_ID);

        then(notificationRepository).should().delete(existing);
    }

    @Test
    @DisplayName("delete de notificacao inexistente lanca NotFoundException")
    void shouldFailDeleteWhenNotificationDoesNotExist() {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(NotFoundException.class);

        then(notificationRepository).should(org.mockito.Mockito.never()).delete(any());
    }

    // ---------------------------------------------------------------- fixtures

    private static Notification notification(final Long userId, final NotificationStatus status) {
        final Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setType(NotificationType.REMINDER);
        notification.setTitle("Titulo");
        notification.setMessage("Mensagem");
        notification.setStatus(status);
        notification.setCreatedAt(CREATED_AT);
        notification.setUser(userId == null ? null : appUser(userId));
        return notification;
    }

    private static NotificationDTO notificationDTO(final Long userId, final NotificationStatus status) {
        final NotificationDTO dto = new NotificationDTO();
        dto.setType(NotificationType.REMINDER);
        dto.setTitle("Titulo");
        dto.setMessage("Mensagem");
        dto.setStatus(status);
        dto.setUser(userId);
        return dto;
    }

    private static AppUser appUser(final Long id) {
        final AppUser user = new AppUser();
        user.setId(id);
        return user;
    }

}
