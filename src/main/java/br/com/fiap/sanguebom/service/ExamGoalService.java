package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.enums.ExamGoalStatus;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import br.com.fiap.sanguebom.model.userexam.ExamGoalDTO;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ExamGoalService {
    static final int DUE_SOON_DAYS = 30;

    private final UserServiceHelper userServiceHelper;
    private final HealthProfileService healthProfileService;
    private final ExamRepository examRepository;
    private final Clock clock;

    public ExamGoalService(final UserServiceHelper userServiceHelper,
            final HealthProfileService healthProfileService,
            final ExamRepository examRepository,
            final Clock clock) {
        this.userServiceHelper = userServiceHelper;
        this.healthProfileService = healthProfileService;
        this.examRepository = examRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExamGoalDTO goalOf(final Long userId) {
        userServiceHelper.getUserByIdOrFail(userId);

        final HealthProfile profile = healthProfileService.getByUserId(userId);
        final ExamPeriodicity periodicity = ExamPeriodicity.from(profile.getExamPeriodicity());

        final LocalDate lastExamDate = examRepository.findFirstByUserIdOrderByCollectedAtDesc(userId)
                .map(Exam::getCollectedAt)
                .map(OffsetDateTime::toLocalDate)
                .orElse(null);

        if (lastExamDate == null) {
            return new ExamGoalDTO(null, null, null, periodicity, ExamGoalStatus.NO_HISTORY);
        }

        final LocalDate dueDate = lastExamDate.plusMonths(periodicity.months());
        final long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(clock), dueDate);
        return new ExamGoalDTO(lastExamDate, dueDate, daysRemaining, periodicity, statusOf(daysRemaining));
    }

    private static ExamGoalStatus statusOf(final long daysRemaining) {
        if (daysRemaining < 0) {
            return ExamGoalStatus.OVERDUE;
        }
        return daysRemaining <= DUE_SOON_DAYS ? ExamGoalStatus.DUE_SOON : ExamGoalStatus.UP_TO_DATE;
    }

}
