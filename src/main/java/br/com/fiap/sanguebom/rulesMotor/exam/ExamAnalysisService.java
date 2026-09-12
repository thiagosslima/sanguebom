package br.com.fiap.sanguebom.rulesMotor.exam;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.*;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisScore;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import br.com.fiap.sanguebom.repository.RuleRepository;
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

    public ExamAnalysisService(
            ReferenceRangeRepository referenceRangeRepository,
            RuleRepository ruleRepository,
            RiskAssessmentClassifier riskAssessmentClassifier) {
        this.referenceRangeRepository = referenceRangeRepository;
        this.ruleRepository = ruleRepository;
        this.riskAssessmentClassifier = riskAssessmentClassifier;
    }

    public ExamAnalysisScore analyze(
            Exam exam,
            List<ExamResult> examResults,
            AppUser user
    ) {

        long userAge = ChronoUnit.YEARS.between(
                user.getBirthDate(),
                LocalDate.now()
        );

        Sex userSex = user.getHealthProfile().getSex();

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
                        new IllegalArgumentException(
                                String.format(
                                        "Nenhuma regra encontrada para o valor %s do item %d",
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
            throw new IllegalArgumentException(
                    "Não é possível calcular o score sem itens avaliados"
            );
        }

        return totalScore.divide(
                BigDecimal.valueOf(evaluatedItems),
                2,
                RoundingMode.HALF_UP
        );
    }
}
