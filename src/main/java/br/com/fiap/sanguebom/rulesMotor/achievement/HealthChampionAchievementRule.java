package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.enums.AchivementCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HealthChampionAchievementRule extends AchievementRuleParent implements AchievementRule  {

    private static final BigDecimal MINIMUM_ELIGIBLE_SCORE = new BigDecimal(3);


    @Override
    public AchivementCode getAchievementCode() {
        return AchivementCode.HEALTH_CHAMPION;
    }

    @Override
    public Boolean isEligible(AchievementContext context) {

        if(alreadyHasAchievement(context)){
            return false;
        }

        BigDecimal score = context.riskAssessment().getScore();

        return score.compareTo(MINIMUM_ELIGIBLE_SCORE) >= 0;
    }
}
