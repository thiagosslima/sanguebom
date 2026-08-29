package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.mapper.UserExamMapperImpl;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.entities.HealthUnit;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import br.com.fiap.sanguebom.repository.HealthProfileRepository;
import br.com.fiap.sanguebom.service.ExamGoalService;
import br.com.fiap.sanguebom.service.HealthProfileService;
import br.com.fiap.sanguebom.service.UserExamService;
import br.com.fiap.sanguebom.service.UserServiceHelper;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.servlet.ServletErrorHandlingConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste dos endpoints de ponta a ponta dentro da aplicacao: rota -> controller -> service -> mapper
 * -> JSON, incluindo o tratamento de excecoes. So os repositorios sao dublados.
 *
 * <p>O advice do starter de error-handling e importado de proposito, para que o slice reproduza a
 * concorrencia de advices que existe em producao.</p>
 */
@WebMvcTest(UserExamResource.class)
@Import({ExamGoalService.class, UserExamService.class, UserExamMapperImpl.class,
        HealthProfileService.class, UserServiceHelper.class, UserExamResourceTest.TestBeans.class})
@ImportAutoConfiguration(ServletErrorHandlingConfiguration.class)
class UserExamResourceTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long EXAM_ID = 10L;
    static final LocalDate TODAY = LocalDate.of(2026, 8, 28);
    private static final OffsetDateTime COLLECTED_AT =
            OffsetDateTime.of(2026, 8, 15, 8, 30, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime RELEASED_AT =
            OffsetDateTime.of(2026, 8, 16, 10, 0, 0, 0, ZoneOffset.UTC);

    @TestConfiguration
    static class TestBeans {

        /** Clock fixo para que as datas da meta sejam deterministicas. */
        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        }

        /** Mesmo basename que o Spring Boot autoconfigura, lendo o messages.properties real. */
        @Bean
        @Primary
        MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("messages");
            messageSource.setDefaultEncoding("UTF-8");
            return messageSource;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamRepository examRepository;

    @MockitoBean
    private ExamResultRepository examResultRepository;

    @MockitoBean
    private HealthProfileRepository healthProfileRepository;

    @MockitoBean
    private AppUserRepository appUserRepository;

    // ---------------------------------------------------------------- CF-331

    @Test
    @DisplayName("GET exam-goal devolve 200 com ultimo exame, vencimento, dias restantes e situacao")
    void shouldReturnExamGoal() throws Exception {
        givenUserExists();
        givenProfile("SEMESTERLY");
        givenLastExamCollectedAt(COLLECTED_AT);

        mockMvc.perform(get("/api/users/{userId}/exam-goal", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastExamDate").value("2026-08-15"))
                .andExpect(jsonPath("$.dueDate").value("2027-02-15"))
                .andExpect(jsonPath("$.daysRemaining").value(171))
                .andExpect(jsonPath("$.periodicity").value("SEMESTERLY"))
                .andExpect(jsonPath("$.status").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("GET exam-goal sem exame nenhum devolve NO_HISTORY com datas nulas")
    void shouldReturnNoHistory() throws Exception {
        givenUserExists();
        givenProfile("YEARLY");
        given(examRepository.findFirstByUserIdOrderByCollectedAtDesc(USER_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/exam-goal", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_HISTORY"))
                .andExpect(jsonPath("$.lastExamDate").doesNotExist())
                .andExpect(jsonPath("$.dueDate").doesNotExist())
                .andExpect(jsonPath("$.daysRemaining").doesNotExist());
    }

    @Test
    @DisplayName("GET exam-goal de usuario inexistente devolve 404 como ProblemDetail")
    void shouldReturnProblemDetailWhenUserDoesNotExist() throws Exception {
        given(appUserRepository.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/exam-goal", 99L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado para o id: 99"));
    }

    @Test
    @DisplayName("GET exam-goal de cidadao sem perfil de saude devolve 404 como ProblemDetail")
    void shouldReturnProblemDetailWhenProfileIsMissing() throws Exception {
        givenUserExists();
        given(healthProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/exam-goal", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Perfil de saúde não encontrado para o usuário: 1"));
    }

    // ---------------------------------------------------------------- CF-332

    @Test
    @DisplayName("GET exams devolve a pagina com o resumo e consulta o repositorio ordenando por coleta desc")
    void shouldReturnPagedHistory() throws Exception {
        given(examRepository.findByUserId(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(exam()), PageRequest.of(0, 1), 3));

        mockMvc.perform(get("/api/users/{userId}/exams?page=0&size=1", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(EXAM_ID))
                .andExpect(jsonPath("$.content[0].status").value("RELEASED"))
                .andExpect(jsonPath("$.content[0].healthUnitName").value("Laboratório Central SP"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(examRepository).should().findByUserId(eq(USER_ID), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("collectedAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("GET exams sem parametros usa a primeira pagina com 10 itens")
    void shouldApplyDefaultPagination() throws Exception {
        given(examRepository.findByUserId(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/users/{userId}/exams", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(examRepository).should().findByUserId(eq(USER_ID), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("GET exams com size invalido devolve 400 como ProblemDetail, nao 500")
    void shouldRejectInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/exams?size=0", USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Parâmetros inválidos"))
                .andExpect(jsonPath("$.['parâmetros inválidos'][0].field").value("size"));

        then(examRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET exams com page negativa devolve 400 como ProblemDetail")
    void shouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/exams?page=-1", USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['parâmetros inválidos'][0].field").value("page"));

        then(examRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET exams com userId nao numerico devolve 400 como ProblemDetail")
    void shouldRejectNonNumericUserId() throws Exception {
        mockMvc.perform(get("/api/users/abc/exams"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetros inválidos"))
                .andExpect(jsonPath("$.['parâmetros inválidos'][0].field").value("userId"));
    }

    // ---------------------------------------------------------------- CF-333

    @Test
    @DisplayName("GET exams/{id} devolve itens, score, nivel, explicacao e o aviso medico acentuado")
    void shouldReturnExamDetail() throws Exception {
        Exam exam = exam();
        exam.setRiskAssessment(risk(new BigDecimal("3.50"), RiskAssessmentLevel.MODERATE,
                "Foram identificadas alterações que indicam necessidade de acompanhamento."));
        given(examRepository.findByIdAndUserId(EXAM_ID, USER_ID)).willReturn(Optional.of(exam));
        given(examResultRepository.findByExamIdWithItem(EXAM_ID)).willReturn(List.of(
                result(new BigDecimal("220.00"), "mg/dL", ExamResultFlag.ATTENTION,
                        item("COL_TOTAL", "Colesterol Total", "mg/dL")),
                result(new BigDecimal("115.00"), null, ExamResultFlag.ALERTA,
                        item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL"))));

        mockMvc.perform(get("/api/users/{userId}/exams/{examId}", USER_ID, EXAM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EXAM_ID))
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].itemCode").value("COL_TOTAL"))
                .andExpect(jsonPath("$.items[0].valueNumeric").value(220.00))
                .andExpect(jsonPath("$.items[0].unit").value("mg/dL"))
                .andExpect(jsonPath("$.items[0].flag").value("ATTENTION"))
                // sem unidade propria, cai para a unidade do catalogo
                .andExpect(jsonPath("$.items[1].unit").value("mg/dL"))
                .andExpect(jsonPath("$.items[1].flag").value("ALERTA"))
                .andExpect(jsonPath("$.riskScore").value(3.50))
                .andExpect(jsonPath("$.riskLevel").value("MODERATE"))
                .andExpect(jsonPath("$.explanation").value(
                        "Foram identificadas alterações que indicam necessidade de acompanhamento."))
                // vem do messages.properties: prova que o encoding foi corrigido
                .andExpect(jsonPath("$.disclaimer").value(
                        "Este é um resultado informativo gerado automaticamente a partir de valores de "
                                + "referência da literatura médica. Não constitui diagnóstico e não "
                                + "substitui a avaliação de um profissional de saúde."));
    }

    @Test
    @DisplayName("GET exams/{id} de exame ainda nao avaliado devolve risco nulo, com itens e aviso")
    void shouldReturnDetailWithoutRiskAssessment() throws Exception {
        given(examRepository.findByIdAndUserId(EXAM_ID, USER_ID)).willReturn(Optional.of(exam()));
        given(examResultRepository.findByExamIdWithItem(EXAM_ID)).willReturn(List.of(
                result(new BigDecimal("88.00"), "mg/dL", ExamResultFlag.IN_ANALYSIS,
                        item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL"))));

        mockMvc.perform(get("/api/users/{userId}/exams/{examId}", USER_ID, EXAM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").doesNotExist())
                .andExpect(jsonPath("$.riskLevel").doesNotExist())
                .andExpect(jsonPath("$.explanation").doesNotExist())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    @DisplayName("GET exams/{id} de exame de outro cidadao devolve 404 e nao vaza dado algum")
    void shouldNotExposeExamOfAnotherCitizen() throws Exception {
        given(examRepository.findByIdAndUserId(EXAM_ID, OTHER_USER_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/exams/{examId}", OTHER_USER_ID, EXAM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Exame não encontrado para o usuário: 2"))
                .andExpect(jsonPath("$.items").doesNotExist())
                .andExpect(jsonPath("$.riskScore").doesNotExist());

        // a consulta precisa ser escopada pelo titular
        then(examRepository).should().findByIdAndUserId(EXAM_ID, OTHER_USER_ID);
        then(examResultRepository).shouldHaveNoInteractions();
    }

    // ---------------------------------------------------------------- fixtures

    private void givenUserExists() {
        AppUser user = new AppUser();
        user.setId(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user));
    }

    private void givenProfile(final String periodicity) {
        HealthProfile profile = new HealthProfile();
        profile.setExamPeriodicity(periodicity);
        given(healthProfileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));
    }

    private void givenLastExamCollectedAt(final OffsetDateTime collectedAt) {
        Exam exam = new Exam();
        exam.setCollectedAt(collectedAt);
        given(examRepository.findFirstByUserIdOrderByCollectedAtDesc(USER_ID)).willReturn(Optional.of(exam));
    }

    private static Exam exam() {
        HealthUnit healthUnit = new HealthUnit();
        healthUnit.setName("Laboratório Central SP");

        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setCollectedAt(COLLECTED_AT);
        exam.setReleasedAt(RELEASED_AT);
        exam.setStatus(ExamStatus.RELEASED);
        exam.setHealthUnit(healthUnit);
        return exam;
    }

    private static ExamItem item(final String code, final String name, final String unit) {
        ExamItem item = new ExamItem();
        item.setCode(code);
        item.setName(name);
        item.setUnit(unit);
        return item;
    }

    private static ExamResult result(final BigDecimal value, final String unit,
            final ExamResultFlag flag, final ExamItem item) {
        ExamResult result = new ExamResult();
        result.setValueNumeric(value);
        result.setUnit(unit);
        result.setFlag(flag);
        result.setExamItem(item);
        return result;
    }

    private static RiskAssessment risk(final BigDecimal score, final RiskAssessmentLevel level,
            final String explanation) {
        RiskAssessment risk = new RiskAssessment();
        risk.setScore(score);
        risk.setLevel(level);
        risk.setExplanation(explanation);
        return risk;
    }

}
