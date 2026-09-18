package br.com.fiap.sanguebom.rulesMotor.exam;

import br.com.fiap.sanguebom.exception.ExamAnalysisException;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.entities.Rule;
import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import br.com.fiap.sanguebom.repository.RuleRepository;
import br.com.fiap.sanguebom.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamAnalysisServiceTest {

    private static final long EXAM_ITEM_ID = 7L;
    private static final long RANGE_ID = 99L;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private RiskAssessmentClassifier riskAssessmentClassifier;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ExamAnalysisService examAnalysisService;

    // --- fixtures -------------------------------------------------------

    private AppUser userWith(LocalDate birthDate, Sex sex) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setBirthDate(birthDate);
        if (sex != null) {
            HealthProfile profile = new HealthProfile();
            profile.setSex(sex);
            user.setHealthProfile(profile);
        }
        return user;
    }

    private ExamResult resultWith(BigDecimal value) {
        ExamItem item = new ExamItem();
        item.setId(EXAM_ITEM_ID);

        ExamResult result = new ExamResult();
        result.setExamItem(item);
        result.setValueNumeric(value);
        return result;
    }

    private Rule ruleWith(BigDecimal min, BigDecimal max, ExamResultFlag level, String score) {
        Rule rule = new Rule();
        rule.setMinValue(min);
        rule.setMaxValue(max);
        rule.setMinInclusive(true);
        rule.setMaxInclusive(true);
        rule.setLevel(level);
        rule.setScore(new BigDecimal(score));
        return rule;
    }

    private void givenRangeExists() {
        ReferenceRange range = new ReferenceRange();
        range.setId(RANGE_ID);
        when(referenceRangeRepository.findApplicableRangeForUserByExamItem(
                eq(EXAM_ITEM_ID), eq(Sex.F), anyLong()))
                .thenReturn(Optional.of(range));
    }

    // --- cálculo do score ----------------------------------------------

    @Test
    @DisplayName("O score final é a média aritmética dos scores das regras aplicáveis")
    void shouldAverageRuleScoresAcrossResults() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), Sex.F);
        givenRangeExists();
        when(ruleRepository.findByReferenceRangeId(RANGE_ID)).thenReturn(List.of(
                ruleWith(BigDecimal.ZERO, new BigDecimal("10"), ExamResultFlag.NORMAL, "2.00"),
                ruleWith(new BigDecimal("10.01"), new BigDecimal("99"), ExamResultFlag.HIGH, "8.00")));

        ExamResult normal = resultWith(new BigDecimal("5"));
        ExamResult high = resultWith(new BigDecimal("50"));

        var score = examAnalysisService.analyze(new Exam(), List.of(normal, high), user);

        // (2.00 + 8.00) / 2
        assertThat(score.finalScore()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("A média usa escala 2 e arredondamento HALF_UP")
    void shouldRoundFinalScoreHalfUpWithTwoDecimals() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), Sex.F);
        givenRangeExists();
        when(ruleRepository.findByReferenceRangeId(RANGE_ID)).thenReturn(List.of(
                ruleWith(BigDecimal.ZERO, new BigDecimal("10"), ExamResultFlag.NORMAL, "1.00"),
                ruleWith(new BigDecimal("10.01"), new BigDecimal("99"), ExamResultFlag.HIGH, "2.00")));

        // (1.00 + 2.00 + 2.00) / 3 = 1.6666... -> 1.67
        var score = examAnalysisService.analyze(
                new Exam(),
                List.of(resultWith(BigDecimal.ONE),
                        resultWith(new BigDecimal("50")),
                        resultWith(new BigDecimal("80"))),
                user);

        assertThat(score.finalScore()).isEqualByComparingTo("1.67");
        assertThat(score.finalScore().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cada resultado recebe a flag da regra que casou com o seu valor")
    void shouldStampMatchingRuleFlagOnEachResult() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), Sex.F);
        givenRangeExists();
        when(ruleRepository.findByReferenceRangeId(RANGE_ID)).thenReturn(List.of(
                ruleWith(BigDecimal.ZERO, new BigDecimal("10"), ExamResultFlag.NORMAL, "1.00"),
                ruleWith(new BigDecimal("10.01"), new BigDecimal("99"), ExamResultFlag.VERY_HIGH, "9.00")));

        ExamResult normal = resultWith(new BigDecimal("3"));
        ExamResult veryHigh = resultWith(new BigDecimal("80"));

        examAnalysisService.analyze(new Exam(), List.of(normal, veryHigh), user);

        assertThat(normal.getFlag()).isEqualTo(ExamResultFlag.NORMAL);
        assertThat(veryHigh.getFlag()).isEqualTo(ExamResultFlag.VERY_HIGH);
    }

    @Test
    @DisplayName("Entre várias regras aplicáveis, vence a primeira retornada pelo repositório")
    void shouldPickFirstApplicableRuleWhenSeveralMatch() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), Sex.F);
        givenRangeExists();
        when(ruleRepository.findByReferenceRangeId(RANGE_ID)).thenReturn(List.of(
                ruleWith(null, null, ExamResultFlag.ATTENTION, "4.00"),
                ruleWith(null, null, ExamResultFlag.NORMAL, "1.00")));

        ExamResult result = resultWith(new BigDecimal("5"));

        var score = examAnalysisService.analyze(new Exam(), List.of(result), user);

        assertThat(result.getFlag()).isEqualTo(ExamResultFlag.ATTENTION);
        assertThat(score.finalScore()).isEqualByComparingTo("4.00");
    }

    // --- idade e sexo ---------------------------------------------------

    @Test
    @DisplayName("A idade enviada ao repositório é a idade em anos completos do usuário")
    void shouldQueryRangeWithCompletedYearsOfAge() {
        LocalDate birth = LocalDate.now().minusYears(40).plusDays(1); // ainda não fez 40
        AppUser user = userWith(birth, Sex.F);

        ReferenceRange range = new ReferenceRange();
        range.setId(RANGE_ID);
        when(referenceRangeRepository.findApplicableRangeForUserByExamItem(EXAM_ITEM_ID, Sex.F, 39L))
                .thenReturn(Optional.of(range));
        when(ruleRepository.findByReferenceRangeId(RANGE_ID)).thenReturn(List.of(
                ruleWith(null, null, ExamResultFlag.NORMAL, "1.00")));

        examAnalysisService.analyze(new Exam(), List.of(resultWith(BigDecimal.ONE)), user);
        // se a idade fosse 40, o stub acima não casaria e o teste falharia em NotFoundException
    }

    // --- caminhos de erro -----------------------------------------------

    @Test
    @DisplayName("Falha quando o usuário não tem data de nascimento")
    void shouldFailWhenBirthDateIsMissing() {
        AppUser user = userWith(null, Sex.F);
        when(messageService.getMessage(ApplicationMessage.EXAM_ANALYSIS_MISSING_BIRTH_DATE))
                .thenReturn("sem data de nascimento");

        assertThatThrownBy(() -> examAnalysisService.analyze(
                new Exam(), List.of(resultWith(BigDecimal.ONE)), user))
                .isInstanceOf(ExamAnalysisException.class)
                .hasMessage("sem data de nascimento");
    }

    @Test
    @DisplayName("Falha quando o usuário não tem perfil de saúde")
    void shouldFailWhenHealthProfileIsMissing() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), null);
        when(messageService.getMessage(ApplicationMessage.EXAM_ANALYSIS_MISSING_HEALTH_PROFILE))
                .thenReturn("sem perfil de saude");

        assertThatThrownBy(() -> examAnalysisService.analyze(
                new Exam(), List.of(resultWith(BigDecimal.ONE)), user))
                .isInstanceOf(ExamAnalysisException.class)
                .hasMessage("sem perfil de saude");
    }

    @Test
    @DisplayName("Falha quando o perfil de saúde existe mas não tem sexo informado")
    void shouldFailWhenSexIsMissing() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), null);
        user.setHealthProfile(new HealthProfile());
        when(messageService.getMessage(ApplicationMessage.EXAM_ANALYSIS_MISSING_SEX))
                .thenReturn("sem sexo");

        assertThatThrownBy(() -> examAnalysisService.analyze(
                new Exam(), List.of(resultWith(BigDecimal.ONE)), user))
                .isInstanceOf(ExamAnalysisException.class)
                .hasMessage("sem sexo");
    }

    @Test
    @DisplayName("Falha com NotFoundException quando não há faixa de referência para o item")
    void shouldFailWhenNoReferenceRangeApplies() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), Sex.F);
        when(referenceRangeRepository.findApplicableRangeForUserByExamItem(
                eq(EXAM_ITEM_ID), eq(Sex.F), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> examAnalysisService.analyze(
                new Exam(), List.of(resultWith(BigDecimal.ONE)), user))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(String.valueOf(EXAM_ITEM_ID));
    }

    @Test
    @DisplayName("Falha quando nenhuma regra da faixa cobre o valor medido")
    void shouldFailWhenNoRuleCoversTheMeasuredValue() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), Sex.F);
        givenRangeExists();
        when(ruleRepository.findByReferenceRangeId(RANGE_ID)).thenReturn(List.of(
                ruleWith(BigDecimal.ZERO, new BigDecimal("10"), ExamResultFlag.NORMAL, "1.00")));
        // Os matchers são tipados de propósito: MessageService tem quatro
        // sobrecargas e um any() sem tipo faz o compilador escolher
        // getMessage(ApplicationMessage, Locale, Object...), que não é a
        // chamada real. O stub cairia no método errado e a mensagem viria null.
        when(messageService.getMessage(
                eq(ApplicationMessage.EXAM_ANALYSIS_RULE_NOT_FOUND),
                any(BigDecimal.class),
                any(Long.class)))
                .thenReturn("regra nao encontrada");

        assertThatThrownBy(() -> examAnalysisService.analyze(
                new Exam(), List.of(resultWith(new BigDecimal("999"))), user))
                .isInstanceOf(ExamAnalysisException.class)
                .hasMessage("regra nao encontrada");
    }

    @Test
    @DisplayName("Falha quando a lista de resultados está vazia, em vez de dividir por zero")
    void shouldFailOnEmptyResultsInsteadOfDividingByZero() {
        AppUser user = userWith(LocalDate.of(1990, 1, 1), Sex.F);
        when(messageService.getMessage(ApplicationMessage.EXAM_ANALYSIS_EMPTY_ITEMS))
                .thenReturn("sem itens");

        assertThatThrownBy(() -> examAnalysisService.analyze(new Exam(), List.of(), user))
                .isInstanceOf(ExamAnalysisException.class)
                .hasMessage("sem itens");
    }
}
