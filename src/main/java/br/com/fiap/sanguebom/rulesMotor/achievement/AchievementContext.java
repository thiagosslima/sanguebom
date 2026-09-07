package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.*;

import java.util.List;

public record AchievementContext(
        AppUser user,
        Exam exam,
        RiskAssessment riskAssessment,
        List<UserAchievement> achievements
) {
}
