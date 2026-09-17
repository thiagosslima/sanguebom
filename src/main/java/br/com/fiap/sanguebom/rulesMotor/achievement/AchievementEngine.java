package br.com.fiap.sanguebom.rulesMotor.achievement;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.*;
import br.com.fiap.sanguebom.repository.AchievementRepository;
import br.com.fiap.sanguebom.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AchievementEngine implements AchievementEvaluator {

    private final List<AchievementRule> achievementRules;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    public AchievementEngine(final List<AchievementRule> achievementRules,
                             final AchievementRepository achievementRepository,
                             final UserAchievementRepository userAchievementRepository) {
        this.achievementRules = achievementRules;
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
    }

    @Override
    public void evaluate(AppUser user, Exam exam, RiskAssessment riskAssessment) {

        AchievementContext context = new AchievementContext(user, exam, riskAssessment);

        for (AchievementRule rule : achievementRules) {

            if (rule.alreadyHasAchievement(context)) {
                continue;
            }

            if (rule.isEligible(context)) {
                grantAchievement(user, rule);
            }

        }

    }

    private void grantAchievement(AppUser user, AchievementRule rule) {
        Achievement achievement = findAchievementOrFail(rule);

        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setId(new UserAchievementId(user.getId(), achievement.getId()));
        userAchievement.setAchievement(achievement);
        userAchievement.setUser(user);
        userAchievementRepository.save(userAchievement);
    }

    private Achievement findAchievementOrFail(AchievementRule rule) {
        return achievementRepository.findByCode(rule.getAchievementCode()).orElseThrow(
                () -> new NotFoundException(
                        String.format("Achievement não configurado para o código: %s ", rule.getAchievementCode())
                )
        );
    }
}
