package br.com.fiap.sanguebom.rulesMotor;

import br.com.fiap.sanguebom.exception.ExamAnalysisException;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.*;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisScore;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import br.com.fiap.sanguebom.repository.RuleRepository;
import br.com.fiap.sanguebom.service.MessageService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ExamAnalysisService {

    private final ReferenceRangeRepository referenceRangeRepository;
    private final RuleRepository ruleRepository;
    private final RiskAssessmentClassifier riskAssessmentClassifier;
    private final MessageService messageService;

    public ExamAnalysisService(
            ReferenceRangeRepository referenceRangeRepository,
            RuleRepository ruleRepository,
            RiskAssessmentClassifier riskAssessmentClassifier,
            MessageService messageService) {
        this.referenceRangeRepository = referenceRangeRepository;
        this.ruleRepository = ruleRepository;
        this.riskAssessmentClassifier = riskAssessmentClassifier;
        this.messageService = messageService;
    }

    public ExamAnalysisScore analyze(
            Exam exam,
            List<ExamResult> examResults,
            AppUser user
    ) {

        long userAge = calculateUserAge(user);

        Sex userSex = getUserSex(user);

        BigDecimal totalScore = BigDecimal.ZERO;

        for (ExamResult examResult : examResults) {

            Rule rule = findApplicableRule(
                    examResult,
                    userSex,
                    userAge
            );

            examResult.setFlag(rule.getLevel());

            totalScore = totalScore.add(
                    rule.getScore()
            );
        }

        BigDecimal finalScore = calculateFinalScore(
                totalScore,
                examResults.size()
        );

        return new ExamAnalysisScore(finalScore);
    }

    private long calculateUserAge(AppUser user) {

        if (user.getBirthDate() == null) {
            throw new ExamAnalysisException(
                    messageService.getMessage("exam.analysis.missing-birth-date")
            );
        }

        return ChronoUnit.YEARS.between(
                user.getBirthDate(),
                LocalDate.now()
        );
    }

    private Sex getUserSex(AppUser user) {

        HealthProfile healthProfile = user.getHealthProfile();

        if (healthProfile == null) {
            throw new ExamAnalysisException(
                    messageService.getMessage("exam.analysis.missing-health-profile")
            );
        }

        if (healthProfile.getSex() == null) {
            throw new ExamAnalysisException(
                    messageService.getMessage("exam.analysis.missing-sex")
            );
        }

        return healthProfile.getSex();
    }

    private Rule findApplicableRule(
            ExamResult examResult,
            Sex userSex,
            long userAge
    ) {

        ReferenceRange referenceRange =
                referenceRangeRepository
                        .findApplicableRangeForUserByExamItem(
                                examResult.getExamItem().getId(),
                                userSex,
                                userAge
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        String.format(
                                                "Faixa de referência não encontrada para o item de exame %d",
                                                examResult.getExamItem().getId()
                                        )
                                )
                        );

        return ruleRepository
                .findByReferenceRangeId(referenceRange.getId())
                .stream()
                .filter(rule ->
                        rule.appliesTo(
                                examResult.getValueNumeric()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new ExamAnalysisException(
                                messageService.getMessage(
                                        "exam.analysis.rule-not-found",
                                        examResult.getValueNumeric(),
                                        examResult.getExamItem().getId()
                                )
                        )
                );
    }

    private BigDecimal calculateFinalScore(
            BigDecimal totalScore,
            int evaluatedItems
    ) {

        if (evaluatedItems == 0) {
            throw new ExamAnalysisException(
                    messageService.getMessage("exam.analysis.empty-items")
            );
        }

        return totalScore.divide(
                BigDecimal.valueOf(evaluatedItems),
                2,
                RoundingMode.HALF_UP
        );
    }
}
