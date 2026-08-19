package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.AppUser;
import br.com.fiap.sanguebom.domain.Notification;
import br.com.fiap.sanguebom.events.BeforeDeleteAppUser;
import br.com.fiap.sanguebom.model.NotificationDTO;
import br.com.fiap.sanguebom.repos.AppUserRepository;
import br.com.fiap.sanguebom.repos.NotificationRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import br.com.fiap.sanguebom.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;

    public NotificationService(final NotificationRepository notificationRepository,
            final AppUserRepository appUserRepository) {
        this.notificationRepository = notificationRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<NotificationDTO> findAll() {
        final List<Notification> notifications = notificationRepository.findAll(Sort.by("id"));
        return notifications.stream()
                .map(notification -> mapToDTO(notification, new NotificationDTO()))
                .toList();
    }

    public NotificationDTO get(final Long id) {
        return notificationRepository.findById(id)
                .map(notification -> mapToDTO(notification, new NotificationDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final NotificationDTO notificationDTO) {
        final Notification notification = new Notification();
        mapToEntity(notificationDTO, notification);
        return notificationRepository.save(notification).getId();
    }

    public void update(final Long id, final NotificationDTO notificationDTO) {
        final Notification notification = notificationRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(notificationDTO, notification);
        notificationRepository.save(notification);
    }

    public void delete(final Long id) {
        final Notification notification = notificationRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        notificationRepository.delete(notification);
    }

    private NotificationDTO mapToDTO(final Notification notification,
            final NotificationDTO notificationDTO) {
        notificationDTO.setId(notification.getId());
        notificationDTO.setType(notification.getType());
        notificationDTO.setTitle(notification.getTitle());
        notificationDTO.setMessage(notification.getMessage());
        notificationDTO.setScheduledAt(notification.getScheduledAt());
        notificationDTO.setSentAt(notification.getSentAt());
        notificationDTO.setStatus(notification.getStatus());
        notificationDTO.setCreatedAt(notification.getCreatedAt());
        notificationDTO.setUser(notification.getUser() == null ? null : notification.getUser().getId());
        return notificationDTO;
    }

    private Notification mapToEntity(final NotificationDTO notificationDTO,
            final Notification notification) {
        notification.setType(notificationDTO.getType());
        notification.setTitle(notificationDTO.getTitle());
        notification.setMessage(notificationDTO.getMessage());
        notification.setScheduledAt(notificationDTO.getScheduledAt());
        notification.setSentAt(notificationDTO.getSentAt());
        notification.setStatus(notificationDTO.getStatus());
        notification.setCreatedAt(notificationDTO.getCreatedAt());
        final AppUser user = notificationDTO.getUser() == null ? null : appUserRepository.findById(notificationDTO.getUser())
                .orElseThrow(() -> new NotFoundException("user not found"));
        notification.setUser(user);
        return notification;
    }

    @EventListener(BeforeDeleteAppUser.class)
    public void on(final BeforeDeleteAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final Notification userNotification = notificationRepository.findFirstByUserId(event.getId());
        if (userNotification != null) {
            referencedException.setKey("appUser.notification.user.referenced");
            referencedException.addParam(userNotification.getId());
            throw referencedException;
        }
    }

}
