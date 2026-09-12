package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.UserAchievement;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import lombok.Getter;

@Getter
public abstract class AchievementRuleParent {

    protected abstract AchivementCode getAchievementCode();

    public boolean alreadyHasAchievement(AchievementContext context) {
        return context.user().getUserAchievements()
                .stream()
                .map(UserAchievement::getAchievement)
                .anyMatch(achievement ->
                        achievement.getCode().equals(getAchievementCode())
                );
    }
}
