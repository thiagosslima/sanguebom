package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.mapper.UserExamMapper;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.entities.HealthUnit;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import br.com.fiap.sanguebom.model.userexam.ExamDetailDTO;
import br.com.fiap.sanguebom.model.userexam.ExamResultItemDTO;
import br.com.fiap.sanguebom.model.userexam.ExamSummaryDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * CF-332 - historico paginado do cidadao. CF-333 - detalhe do exame com o pre-diagnostico.
 */
@ExtendWith(MockitoExtension.class)
class UserExamServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long EXAM_ID = 10L;
    private static final Locale LOCALE = Locale.of("pt", "BR");
    private static final String DISCLAIMER = "Nao constitui diagnostico e nao substitui a avaliacao "
            + "de um profissional de saude.";
    private static final OffsetDateTime COLLECTED_AT =
            OffsetDateTime.of(2026, 8, 15, 8, 30, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime RELEASED_AT =
            OffsetDateTime.of(2026, 8, 16, 10, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamResultRepository examResultRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private UserExamService service;

    @BeforeEach
    void setUp() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("exam.medical-disclaimer", LOCALE, DISCLAIMER);
        // mapper real, para o mapeamento ser de fato exercitado
        service = new UserExamService(examRepository, examResultRepository,
                Mappers.getMapper(UserExamMapper.class), new MessageService(messageSource));
    }

    // ---------------------------------------------------------------- CF-332

    @Test
    @DisplayName("historico e consultado do exame mais recente para o mais antigo, pela data de coleta")
    void shouldQueryHistoryOrderedByCollectedAtDesc() {
        given(examRepository.findByUserId(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        service.history(USER_ID, 2, 5);

        then(examRepository).should().findByUserId(eq(USER_ID), pageableCaptor.capture());
        final Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("collectedAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("historico traz o resumo de cada exame e os metadados da pagina")
    void shouldMapHistoryPage() {
        // 3 exames no total, 1 por pagina: os metadados precisam chegar intactos ao cliente
        given(examRepository.findByUserId(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(exam()), PageRequest.of(0, 1), 3));

        final PageResponse<ExamSummaryDTO> response = service.history(USER_ID, 0, 1);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.content()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(EXAM_ID);
            assertThat(summary.collectedAt()).isEqualTo(COLLECTED_AT);
            assertThat(summary.releasedAt()).isEqualTo(RELEASED_AT);
            assertThat(summary.status()).isEqualTo(ExamStatus.RELEASED);
            assertThat(summary.healthUnitName()).isEqualTo("Laboratorio Central SP");
        });
    }

    @Test
    @DisplayName("cidadao sem exames recebe uma pagina vazia, e nao erro")
    void shouldReturnEmptyHistory() {
        given(examRepository.findByUserId(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        final PageResponse<ExamSummaryDTO> response = service.history(USER_ID, 0, 10);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    // ---------------------------------------------------------------- CF-333

    @Test
    @DisplayName("detalhe traz cada item com valor, unidade e classificacao, mais score, explicacao e aviso medico")
    void shouldReturnExamDetailWithItemsAndRiskAssessment() {
        final Exam exam = exam();
        exam.setRiskAssessment(risk(new BigDecimal("3.50"), RiskAssessmentLevel.MODERATE,
                "Foram identificadas alteracoes que indicam necessidade de acompanhamento."));
        given(examRepository.findByIdAndUserId(EXAM_ID, USER_ID)).willReturn(Optional.of(exam));
        given(examResultRepository.findByExamIdWithItem(EXAM_ID)).willReturn(List.of(
                result(new BigDecimal("220.00"), "mg/dL", ExamResultFlag.ATTENTION,
                        item("COL_TOTAL", "Colesterol Total", "mg/dL")),
                result(new BigDecimal("115.00"), "mg/dL", ExamResultFlag.ATTENTION,
                        item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL"))));

        final ExamDetailDTO detail = service.detail(USER_ID, EXAM_ID, LOCALE);

        assertThat(detail.id()).isEqualTo(EXAM_ID);
        assertThat(detail.collectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(detail.status()).isEqualTo(ExamStatus.RELEASED);
        assertThat(detail.healthUnitName()).isEqualTo("Laboratorio Central SP");
        assertThat(detail.items())
                .extracting(ExamResultItemDTO::itemCode, ExamResultItemDTO::itemName,
                        ExamResultItemDTO::valueNumeric, ExamResultItemDTO::unit, ExamResultItemDTO::flag)
                .containsExactly(
                        tuple("COL_TOTAL", "Colesterol Total", new BigDecimal("220.00"), "mg/dL",
                                ExamResultFlag.ATTENTION),
                        tuple("GLI_JEJUM", "Glicemia em Jejum", new BigDecimal("115.00"), "mg/dL",
                                ExamResultFlag.ATTENTION));
        assertThat(detail.riskScore()).isEqualByComparingTo("3.50");
        assertThat(detail.riskLevel()).isEqualTo(RiskAssessmentLevel.MODERATE);
        assertThat(detail.explanation()).contains("necessidade de acompanhamento");
        assertThat(detail.disclaimer()).isEqualTo(DISCLAIMER);
    }

    @Test
    @DisplayName("resultado sem unidade propria usa a unidade cadastrada no item do catalogo")
    void shouldFallBackToCatalogUnit() {
        given(examRepository.findByIdAndUserId(EXAM_ID, USER_ID)).willReturn(Optional.of(exam()));
        given(examResultRepository.findByExamIdWithItem(EXAM_ID)).willReturn(List.of(
                result(new BigDecimal("88.00"), null, ExamResultFlag.NORMAL,
                        item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL"))));

        final ExamDetailDTO detail = service.detail(USER_ID, EXAM_ID, LOCALE);

        assertThat(detail.items()).singleElement()
                .extracting(ExamResultItemDTO::unit).isEqualTo("mg/dL");
    }

    @Test
    @DisplayName("exame ainda nao avaliado pelo motor de regras devolve risco nulo, sem perder itens nem aviso")
    void shouldReturnDetailWithoutRiskAssessment() {
        given(examRepository.findByIdAndUserId(EXAM_ID, USER_ID)).willReturn(Optional.of(exam()));
        given(examResultRepository.findByExamIdWithItem(EXAM_ID)).willReturn(List.of(
                result(new BigDecimal("88.00"), "mg/dL", ExamResultFlag.IN_ANALYSIS,
                        item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL"))));

        final ExamDetailDTO detail = service.detail(USER_ID, EXAM_ID, LOCALE);

        assertThat(detail.riskScore()).isNull();
        assertThat(detail.riskLevel()).isNull();
        assertThat(detail.explanation()).isNull();
        assertThat(detail.items()).singleElement().satisfies(item -> {
            assertThat(item.valueNumeric()).isEqualByComparingTo("88.00");
            assertThat(item.flag()).isEqualTo(ExamResultFlag.IN_ANALYSIS);
        });
        assertThat(detail.disclaimer()).isEqualTo(DISCLAIMER);
    }

    @Test
    @DisplayName("exame de outro cidadao nao e encontrado: nenhum dado alheio vaza")
    void shouldNotExposeExamOfAnotherCitizen() {
        given(examRepository.findByIdAndUserId(EXAM_ID, OTHER_USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(OTHER_USER_ID, EXAM_ID, LOCALE))
                .isInstanceOf(NotFoundException.class);

        // a consulta precisa ser escopada pelo titular: buscar so por id vazaria o exame alheio
        then(examRepository).should().findByIdAndUserId(EXAM_ID, OTHER_USER_ID);
        then(examResultRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("exame inexistente resulta em 404")
    void shouldFailWhenExamDoesNotExist() {
        given(examRepository.findByIdAndUserId(999L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(USER_ID, 999L, LOCALE))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- fixtures

    private static Exam exam() {
        final HealthUnit healthUnit = new HealthUnit();
        healthUnit.setName("Laboratorio Central SP");

        final Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setCollectedAt(COLLECTED_AT);
        exam.setReleasedAt(RELEASED_AT);
        exam.setStatus(ExamStatus.RELEASED);
        exam.setHealthUnit(healthUnit);
        return exam;
    }

    private static ExamItem item(final String code, final String name, final String unit) {
        final ExamItem item = new ExamItem();
        item.setCode(code);
        item.setName(name);
        item.setUnit(unit);
        return item;
    }

    private static ExamResult result(final BigDecimal value, final String unit,
            final ExamResultFlag flag, final ExamItem item) {
        final ExamResult result = new ExamResult();
        result.setValueNumeric(value);
        result.setUnit(unit);
        result.setFlag(flag);
        result.setExamItem(item);
        return result;
    }

    private static RiskAssessment risk(final BigDecimal score, final RiskAssessmentLevel level,
            final String explanation) {
        final RiskAssessment risk = new RiskAssessment();
        risk.setScore(score);
        risk.setLevel(level);
        risk.setExplanation(explanation);
        return risk;
    }

}
