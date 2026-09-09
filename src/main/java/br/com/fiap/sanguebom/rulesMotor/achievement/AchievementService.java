package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.*;
import br.com.fiap.sanguebom.repository.AchievementRepository;
import br.com.fiap.sanguebom.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AchievementService implements AchievementEvaluator {

    private final List<AchievementRule> achievementRules;
    private AchievementRepository achievementRepository;
    private UserAchievementRepository userAchievementRepository;

    public AchievementService(List<AchievementRule> achievementRules) {
        this.achievementRules = achievementRules;
    }


    @Override
    public void evaluate(AppUser user, Exam exam, RiskAssessment riskAssessment) {

        AchievementContext context = new AchievementContext(user, exam, riskAssessment);

        for(AchievementRule rule : achievementRules) {

            if(rule.alreadyHasAchievement(context)) {
                continue;
            }

            if(rule.isEligible(context)) {
                grantAchievement(user, rule);
            }

        }

    }

    private void grantAchievement(AppUser user, AchievementRule rule) {
        Achievement achievement = findAchievementOrFail(rule);

        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setAchievement(achievement);
        userAchievement.setUser(user);
        userAchievementRepository.save(userAchievement);
    }

    private Achievement findAchievementOrFail(AchievementRule rule) {
        Achievement achievement = achievementRepository.findByCode(rule.getAchievementCode()).orElseThrow(
                ()-> new NotFoundException(
                        String.format("Achievement não configurado para o código: %s ", rule.getAchievementCode())
                )
        );
        return achievement;
    }
}
