package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ExamNotificationService {

    static final String TITLE_KEY = "notification.exam-result-available.title";
    static final String MESSAGE_KEY = "notification.exam-result-available.message";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificationDispatcher dispatcher;
    private final MessageSource messageSource;

    public ExamNotificationService(final NotificationDispatcher dispatcher,
            final MessageSource messageSource) {
        this.dispatcher = dispatcher;
        this.messageSource = messageSource;
    }

    public static String referenceKeyOf(final Long examId) {
        return "EXAM:" + examId;
    }

    public void notifyResultAvailable(final Exam exam) {
        final Locale locale = Locale.getDefault();
        final OffsetDateTime collectedAt = exam.getCollectedAt();
        final Object[] args = {collectedAt == null ? "-" : DATE.format(collectedAt)};

        dispatcher.dispatch(exam.getUser(), NotificationType.EXAM_RESULT_AVAILABLE,
                messageSource.getMessage(TITLE_KEY, null, locale),
                messageSource.getMessage(MESSAGE_KEY, args, locale),
                referenceKeyOf(exam.getId()));
    }
}
