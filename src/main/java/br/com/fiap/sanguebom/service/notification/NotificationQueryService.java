package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.notification.NotificationSummaryDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.NotificationRepository;
import br.com.fiap.sanguebom.service.UserServiceHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final UserServiceHelper userServiceHelper;

    public NotificationQueryService(final NotificationRepository notificationRepository,
            final UserServiceHelper userServiceHelper) {
        this.notificationRepository = notificationRepository;
        this.userServiceHelper = userServiceHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationSummaryDTO> history(final Long userId, final int page, final int size) {
        userServiceHelper.getUserByIdOrFail(userId);

        return PageResponse.of(notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationSummaryDTO::of));
    }
}
