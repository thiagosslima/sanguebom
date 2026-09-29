package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.BadRequestException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.dtos.NotificationDTO;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;

    public List<NotificationDTO> findAll(final Long appUserId, final NotificationStatus status) {
        final List<Notification> notifications = notificationRepository.findAll(Sort.by("id"));

        return notifications.stream()
                .filter(notification -> appUserId == null
                        || (notification.getUser() != null && notification.getUser().getId().equals(appUserId)))
                .filter(notification -> status == null || notification.getStatus() == status)
                .map(notification -> mapToDTO(notification, new NotificationDTO()))
                .toList();
    }

    public NotificationDTO get(Long id) {
        return notificationRepository.findById(id)
                .map(notification -> mapToDTO(notification, new NotificationDTO()))
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
    }

    public Long create(final NotificationDTO notificationDTO) {
        final Notification notification = new Notification();

        try{
            mapToEntity(notificationDTO, notification);
            return notificationRepository.save(notification).getId();
        } catch (DataAccessException e) {
            throw new BadRequestException("Error ao criar a notificação: " + e.getMessage());
        }
    }

    public void update(final Long id, final NotificationDTO notificationDTO) {
        final Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada"));

        try{
            mapToEntity(notificationDTO, notification);
            notificationRepository.save(notification);
        } catch (DataAccessException e) {
            throw new BadRequestException("Error ao atualizar a notificação: " + e.getMessage());
        }
    }

    public void delete(final Long id) {
        final Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
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
        notificationDTO.setReferenceKey(notification.getReferenceKey());
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
        notification.setReferenceKey(notificationDTO.getReferenceKey());
        notification.setCreatedAt(notificationDTO.getCreatedAt());
        final AppUser user = notificationDTO.getUser() == null ? null : appUserRepository.findById(notificationDTO.getUser())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        notification.setUser(user);
        return notification;
    }
}
