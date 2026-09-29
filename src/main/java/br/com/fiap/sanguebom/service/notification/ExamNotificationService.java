package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.service.MessageService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ExamNotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificationDispatcher dispatcher;
    private final MessageService messageService;

    public ExamNotificationService(final NotificationDispatcher dispatcher,
            final MessageService messageService) {
        this.dispatcher = dispatcher;
        this.messageService = messageService;
    }

    public static String referenceKeyOf(final Long examId) {
        return "EXAM:" + examId;
    }

    public void notifyResultAvailable(final Exam exam) {
        final OffsetDateTime collectedAt = exam.getCollectedAt();
        final Object[] args = {collectedAt == null ? "-" : DATE.format(collectedAt)};

        dispatcher.dispatch(exam.getUser(), NotificationType.EXAM_RESULT_AVAILABLE,
                messageService.getMessage(ApplicationMessage.NOTIFICATION_EXAM_RESULT_AVAILABLE_TITLE),
                messageService.getMessage(ApplicationMessage.NOTIFICATION_EXAM_RESULT_AVAILABLE_MESSAGE, args),
                referenceKeyOf(exam.getId()));
    }
}
