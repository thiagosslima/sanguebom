package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.mapper.ExamAnalysisResultMapper;
import br.com.fiap.sanguebom.mapper.ExamIResultMapper;
import br.com.fiap.sanguebom.mapper.ExamMapper;
import br.com.fiap.sanguebom.exception.DuplicatedExamResultException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.entities.HealthUnit;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisResultDTO;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisScore;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import br.com.fiap.sanguebom.repository.HealthUnitRepository;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import br.com.fiap.sanguebom.repository.RiskAssessmentRepository;
import br.com.fiap.sanguebom.repository.RuleRepository;
import br.com.fiap.sanguebom.rulesMotor.ExamAnalysisService;
import br.com.fiap.sanguebom.service.notification.ExamNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * CF-348 - o exame nasce liberado e o cidadao e avisado do resultado disponivel.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamServiceCreateNotificationTest {

    private static final Long USER_ID = 1L;
    private static final Long HEALTH_UNIT_ID = 1L;
    private static final Long EXAM_ITEM_ID = 1L;
    private static final Long EXAM_ID = 10001L;
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 9, 7, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime COLLECTED_AT =
            OffsetDateTime.of(2026, 9, 6, 8, 30, 0, 0, ZoneOffset.UTC);

    @Mock private ExamRepository examRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private HealthUnitRepository healthUnitRepository;
    @Mock private UserServiceHelper userServiceHelper;
    @Mock private ExamItemRepository examItemRepository;
    @Mock private ExamMapper examMapper;
    @Mock private ExamResultRepository examResultRepository;
    @Mock private ReferenceRangeRepository referenceRangeRepository;
    @Mock private RuleRepository ruleRepository;
    @Mock private RiskAssessmentRepository riskAssessmentRepository;
    @Mock private RiskAssessmentService riskAssessmentService;
    @Mock private ExamAnalysisService examAnalysisService;
    @Mock private ExamIResultMapper examIResultMapper;
    @Mock private ExamAnalysisResultMapper examAnalysisResultMapper;
    @Mock private ExamNotificationService examNotificationService;

    private ExamService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new ExamService(examRepository, appUserRepository, healthUnitRepository,
                userServiceHelper, examItemRepository, examMapper, examResultRepository,
                referenceRangeRepository, ruleRepository, riskAssessmentRepository,
                riskAssessmentService, examAnalysisService, examIResultMapper,
                examAnalysisResultMapper, examNotificationService,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));

        user = new AppUser();
        user.setId(USER_ID);
        givenHappyPath();
    }

    @Test
    @DisplayName("o exame criado ja nasce RELEASED, com a data de liberacao do relogio da aplicacao")
    void shouldCreateExamAlreadyReleased() {
        service.create(examCreateDTO());

        final ArgumentCaptor<Exam> captor = ArgumentCaptor.forClass(Exam.class);
        then(examRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExamStatus.RELEASED);
        assertThat(captor.getValue().getReleasedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("com o exame liberado, o cidadao dono do exame recebe o aviso de resultado disponivel")
    void shouldNotifyExamOwnerAfterCreation() {
        service.create(examCreateDTO());

        final ArgumentCaptor<Exam> captor = ArgumentCaptor.forClass(Exam.class);
        then(examNotificationService).should().notifyResultAvailable(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(EXAM_ID);
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("exame com item duplicado falha e nao notifica ninguem")
    void shouldNotNotifyWhenExamIsInvalid() {
        final ExamCreateDTO invalid = new ExamCreateDTO(COLLECTED_AT, "LAB-1", USER_ID,
                HEALTH_UNIT_ID, List.of(
                new br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO(EXAM_ITEM_ID, BigDecimal.TEN),
                new br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO(EXAM_ITEM_ID, BigDecimal.ONE)));

        try {
            service.create(invalid);
        } catch (final DuplicatedExamResultException expected) {
            // esperado: itens de exame duplicados
        }

        then(examNotificationService).shouldHaveNoInteractions();
    }

    private void givenHappyPath() {
        final HealthUnit healthUnit = new HealthUnit();
        healthUnit.setId(HEALTH_UNIT_ID);
        final ExamItem examItem = new ExamItem();
        examItem.setId(EXAM_ITEM_ID);
        examItem.setUnit("mg/dL");

        given(userServiceHelper.getUserByIdOrFail(USER_ID)).willReturn(user);
        given(healthUnitRepository.findById(HEALTH_UNIT_ID)).willReturn(Optional.of(healthUnit));
        given(examItemRepository.findActiveById(EXAM_ITEM_ID)).willReturn(Optional.of(examItem));
        given(examMapper.toEntity(any(ExamCreateDTO.class))).willAnswer(invocation -> {
            final Exam exam = new Exam();
            exam.setCollectedAt(COLLECTED_AT);
            return exam;
        });
        given(examRepository.save(any(Exam.class))).willAnswer(invocation -> {
            final Exam exam = invocation.getArgument(0);
            exam.setId(EXAM_ID);
            return exam;
        });
        given(examIResultMapper.toEntity(any())).willAnswer(invocation -> new ExamResult());
        given(examResultRepository.saveAll(any())).willAnswer(invocation -> {
            final List<ExamResult> results = invocation.getArgument(0);
            return results;
        });
        given(examAnalysisService.analyze(any(), any(), any()))
                .willReturn(new ExamAnalysisScore(BigDecimal.ZERO));
        given(riskAssessmentService.createRiskAssessment(any(), any(), any()))
                .willReturn(new RiskAssessment());
        given(riskAssessmentRepository.save(any(RiskAssessment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(examAnalysisResultMapper.fromRiskAssessmentToAnalysisResult(any()))
                .willReturn(new ExamAnalysisResultDTO(1L, BigDecimal.ZERO, null, "ok", NOW,
                        USER_ID, EXAM_ID, HEALTH_UNIT_ID));
    }

    private static ExamCreateDTO examCreateDTO() {
        return new ExamCreateDTO(COLLECTED_AT, "LAB-1", USER_ID, HEALTH_UNIT_ID,
                List.of(new br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO(EXAM_ITEM_ID,
                        BigDecimal.valueOf(95))));
    }
}
