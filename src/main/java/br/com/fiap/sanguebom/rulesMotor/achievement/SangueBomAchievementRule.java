package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.UserAchievement;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.springframework.stereotype.Component;

@Component
public class SangueBomAchievementRule extends AchievementRuleParent implements AchievementRule{

    private final ExamRepository examRepository;

    public SangueBomAchievementRule(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    public AchivementCode getAchievementCode() {
        return AchivementCode.SANGUE_BOM;
    }

    @Override
    public Boolean isEligible(AchievementContext context) {

    long processed = examRepository.countByStatusAndUserId(
            context.user().getId(),
            ExamStatus.RELEASED);

    if(alreadyHasAchievement(context)){
        return false;
    }

        return processed>=1;
    }
}
