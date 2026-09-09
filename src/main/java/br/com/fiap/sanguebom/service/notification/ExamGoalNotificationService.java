package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.config.NotificationProperties;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import br.com.fiap.sanguebom.model.notification.ExamGoalScanResultDTO;
import br.com.fiap.sanguebom.model.userexam.ExamGoalDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.service.ExamGoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ExamGoalNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(ExamGoalNotificationService.class);

    static final String ACTIVE_STATUS = "ACTIVE";
    static final String DUE_SOON_TITLE_KEY = "notification.exam-goal.due-soon.title";
    static final String DUE_SOON_MESSAGE_KEY = "notification.exam-goal.due-soon.message";
    static final String OVERDUE_TITLE_KEY = "notification.exam-goal.overdue.title";
    static final String OVERDUE_MESSAGE_KEY = "notification.exam-goal.overdue.message";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Sort BY_ID = Sort.by("id");

    private final AppUserRepository appUserRepository;
    private final ExamGoalService examGoalService;
    private final NotificationDispatcher dispatcher;
    private final MessageSource messageSource;
    private final NotificationProperties properties;

    public ExamGoalNotificationService(final AppUserRepository appUserRepository,
            final ExamGoalService examGoalService,
            final NotificationDispatcher dispatcher,
            final MessageSource messageSource,
            final NotificationProperties properties) {
        this.appUserRepository = appUserRepository;
        this.examGoalService = examGoalService;
        this.dispatcher = dispatcher;
        this.messageSource = messageSource;
        this.properties = properties;
    }

    public ExamGoalScanResultDTO scanActiveUsers() {
        final int pageSize = properties.examGoal().pageSize();
        int scanned = 0;
        int dueSoon = 0;
        int overdue = 0;
        int skipped = 0;
        int pageNumber = 0;
        Page<AppUser> page;

        do {
            page = appUserRepository.findByStatusIgnoreCase(ACTIVE_STATUS,
                    PageRequest.of(pageNumber, pageSize, BY_ID));

            for (final AppUser user : page.getContent()) {
                scanned++;
                switch (notifyGoalOf(user)) {
                    case DUE_SOON_CREATED -> dueSoon++;
                    case OVERDUE_CREATED -> overdue++;
                    case SKIPPED -> skipped++;
                    default -> {
                    }
                }
            }
            pageNumber++;
        } while (page.hasNext());

        LOG.info("Varredura da meta de exames: {} cidadaos, {} avisos de vencimento proximo, "
                + "{} avisos de meta vencida, {} ignorados", scanned, dueSoon, overdue, skipped);
        return new ExamGoalScanResultDTO(scanned, dueSoon, overdue, skipped);
    }

    private ScanOutcome notifyGoalOf(final AppUser user) {
        try {
            final ExamGoalDTO goal = examGoalService.goalOf(user.getId());
            return switch (goal.status()) {
                case DUE_SOON -> dispatchDueSoon(user, goal);
                case OVERDUE -> dispatchOverdue(user, goal);
                default -> ScanOutcome.NOTHING_TO_DO;
            };
        } catch (final NotFoundException exception) {
            LOG.warn("Cidadao {} ignorado na varredura da meta: {}", user.getId(), exception.getMessage());
            return ScanOutcome.SKIPPED;
        } catch (final DataIntegrityViolationException exception) {
            LOG.debug("Aviso do cidadao {} ja criado por outra execucao", user.getId());
            return ScanOutcome.NOTHING_TO_DO;
        }
    }

    private ScanOutcome dispatchDueSoon(final AppUser user, final ExamGoalDTO goal) {
        final Locale locale = Locale.getDefault();
        final Object[] args = {goal.daysRemaining(), DATE.format(goal.dueDate())};

        return dispatcher.dispatch(user, NotificationType.EXAM_GOAL_DUE_SOON,
                messageSource.getMessage(DUE_SOON_TITLE_KEY, null, locale),
                messageSource.getMessage(DUE_SOON_MESSAGE_KEY, args, locale),
                cycleKeyOf(goal.dueDate()))
                .map(notification -> ScanOutcome.DUE_SOON_CREATED)
                .orElse(ScanOutcome.NOTHING_TO_DO);
    }

    private ScanOutcome dispatchOverdue(final AppUser user, final ExamGoalDTO goal) {
        final Locale locale = Locale.getDefault();
        final Object[] args = {DATE.format(goal.dueDate())};

        return dispatcher.dispatch(user, NotificationType.EXAM_GOAL_OVERDUE,
                messageSource.getMessage(OVERDUE_TITLE_KEY, null, locale),
                messageSource.getMessage(OVERDUE_MESSAGE_KEY, args, locale),
                cycleKeyOf(goal.dueDate()))
                .map(notification -> ScanOutcome.OVERDUE_CREATED)
                .orElse(ScanOutcome.NOTHING_TO_DO);
    }

    static String cycleKeyOf(final LocalDate dueDate) {
        return dueDate.toString();
    }

    private enum ScanOutcome {
        DUE_SOON_CREATED,
        OVERDUE_CREATED,
        SKIPPED,
        NOTHING_TO_DO
    }
}
