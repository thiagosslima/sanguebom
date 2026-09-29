package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.UserAchievement;
import br.com.fiap.sanguebom.model.enums.AchivementCode;

public abstract class AchievementRuleParent implements AchievementRule {

    @Override
    public abstract AchivementCode getAchievementCode();

    @Override
    public boolean alreadyHasAchievement(AchievementContext context) {
        return context.user().getUserAchievements()
                .stream()
                .map(UserAchievement::getAchievement)
                .anyMatch(achievement ->
                        achievement.getCode().equals(getAchievementCode())
                );
    }
}
