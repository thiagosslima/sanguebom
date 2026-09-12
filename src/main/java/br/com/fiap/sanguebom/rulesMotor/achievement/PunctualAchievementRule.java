package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PunctualAchievementRule extends AchievementRuleParent implements AchievementRule {

    private final ExamRepository examRepository;

    private static final int TOLERANCE_DAYS = 30;
    private static final int MINIMUM_EXAM_QUANTITY = 2;


    public PunctualAchievementRule(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    public AchivementCode getAchievementCode() {
        return AchivementCode.PUNCTUAL;
    }

    @Override
    public boolean isEligible(AchievementContext context) {

        List<Exam> exams = examRepository.findLatestReleasedByUserId(context.user().getId());

        if(exams.size() < MINIMUM_EXAM_QUANTITY){
            return false;
        }

        Exam lastExam = exams.get(0);
        Exam previusExam = exams.get(1);

        ExamPeriodicity periodicity =
                context.user()
                        .getHealthProfile()
                        .getExamPeriodicity();

        LocalDate deadLine = calculateDeadLine(previusExam.getCollectedAt(), periodicity);

        boolean isPunctual = lastExam.getCollectedAt().toLocalDate().isBefore(deadLine);

        return isPunctual;
    }

    private LocalDate calculateDeadLine(OffsetDateTime previousExamDate, ExamPeriodicity periodicity) {

        LocalDate date = previousExamDate.toLocalDate().plusDays(TOLERANCE_DAYS);

        return switch (periodicity) {
            case QUARTERLY -> date.plusMonths(3);
            case SEMESTERLY -> date.plusMonths(6);
            case YEARLY -> date.plusMonths(12);
        };
    }

}
