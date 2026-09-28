package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;

public interface AchievementEvaluator {

    void evaluate(AppUser user, Exam exam, RiskAssessment riskAssessment);
}
