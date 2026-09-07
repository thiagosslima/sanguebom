package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.enums.AchivementCode;

public interface AchievementRule {

    AchivementCode getAchievementCode();

    Boolean isEligible(AchievementContext context);

}
