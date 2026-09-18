package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PunctualAchievementRule extends AchievementRuleParent {

    private static final int TOLERANCE_DAYS = 30;
    private static final int MINIMUM_EXAM_QUANTITY = 2;

    private final ExamRepository examRepository;

    public PunctualAchievementRule(final ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    public AchivementCode getAchievementCode() {
        return AchivementCode.PUNCTUAL;
    }

    @Override
    public boolean isEligible(AchievementContext context) {

        ExamPeriodicity periodicity = periodicityOf(context);

        if (periodicity == null) {
            return false;
        }

        List<Exam> exams = examRepository.findTop2ByUserIdAndStatusOrderByCollectedAtDesc(
                context.user().getId(),
                ExamStatus.RELEASED);

        if (exams.size() < MINIMUM_EXAM_QUANTITY) {
            return false;
        }

        Exam lastExam = exams.get(0);
        Exam previousExam = exams.get(1);

        LocalDate deadLine = calculateDeadLine(previousExam.getCollectedAt(), periodicity);

        return lastExam.getCollectedAt().toLocalDate().isBefore(deadLine);
    }

    private ExamPeriodicity periodicityOf(AchievementContext context) {

        HealthProfile healthProfile = context.user().getHealthProfile();

        return healthProfile == null ? null : healthProfile.getExamPeriodicity();
    }

    private LocalDate calculateDeadLine(OffsetDateTime previousExamDate, ExamPeriodicity periodicity) {

        return previousExamDate.toLocalDate()
                .plusMonths(periodicity.months())
                .plusDays(TOLERANCE_DAYS);
    }

}
