package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.enums.AchivementCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HealthChampionAchievementRule extends AchievementRuleParent {

    private static final BigDecimal MAXIMUM_ELIGIBLE_SCORE = new BigDecimal(3);

    @Override
    public AchivementCode getAchievementCode() {
        return AchivementCode.HEALTH_CHAMPION;
    }

    @Override
    public boolean isEligible(AchievementContext context) {

        if (context.riskAssessment() == null || context.riskAssessment().getScore() == null) {
            return false;
        }

        BigDecimal score = context.riskAssessment().getScore();

        return score.compareTo(MAXIMUM_ELIGIBLE_SCORE) <= 0;
    }
}
