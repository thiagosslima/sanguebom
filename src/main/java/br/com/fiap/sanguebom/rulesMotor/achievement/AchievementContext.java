package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.*;

public record AchievementContext(
        AppUser user,
        Exam exam,
        RiskAssessment riskAssessment
) {
}
