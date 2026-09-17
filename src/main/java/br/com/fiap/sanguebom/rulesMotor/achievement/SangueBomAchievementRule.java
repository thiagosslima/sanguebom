package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.springframework.stereotype.Component;

@Component
public class SangueBomAchievementRule extends AchievementRuleParent {

    private static final int MINIMUM_EXAM_QUANTITY = 1;

    private final ExamRepository examRepository;

    public SangueBomAchievementRule(final ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    public AchivementCode getAchievementCode() {
        return AchivementCode.SANGUE_BOM;
    }

    @Override
    public boolean isEligible(AchievementContext context) {

        long processed = examRepository.countByUserIdAndStatus(
                context.user().getId(),
                ExamStatus.RELEASED);

        return processed >= MINIMUM_EXAM_QUANTITY;
    }
}
