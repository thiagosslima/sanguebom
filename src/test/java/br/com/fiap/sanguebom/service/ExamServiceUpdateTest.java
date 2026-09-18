package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.mapper.ExamAnalysisResultMapper;
import br.com.fiap.sanguebom.mapper.ExamIResultMapper;
import br.com.fiap.sanguebom.mapper.ExamMapper;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.HealthUnit;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.model.exam.ExamRecoverDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import br.com.fiap.sanguebom.repository.HealthUnitRepository;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import br.com.fiap.sanguebom.repository.RiskAssessmentRepository;
import br.com.fiap.sanguebom.repository.RuleRepository;
import br.com.fiap.sanguebom.rulesMotor.achievement.AchievementEvaluator;
import br.com.fiap.sanguebom.rulesMotor.exam.ExamAnalysisService;
import br.com.fiap.sanguebom.service.notification.ExamNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * O update precisa alterar o exame do path. Antes ele montava uma entidade nova a partir do DTO,
 * que nao traz id, entao o save() virava INSERT: respondia 200, nao mudava nada e deixava para tras
 * um exame sem cidadao e sem unidade de saude.
 */
@ExtendWith(MockitoExtension.class)
class ExamServiceUpdateTest {

    private static final Long EXAM_ID = 10001L;
    private static final Long USER_ID = 1L;
    private static final Long HEALTH_UNIT_ID = 2L;
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 9, 7, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime COLLECTED_AT =
            OffsetDateTime.of(2026, 9, 6, 8, 30, 0, 0, ZoneOffset.UTC);

    @Mock private ExamRepository examRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private HealthUnitRepository healthUnitRepository;
    @Mock private UserServiceHelper userServiceHelper;
    @Mock private ExamItemRepository examItemRepository;
    @Mock private ExamResultRepository examResultRepository;
    @Mock private ReferenceRangeRepository referenceRangeRepository;
    @Mock private RuleRepository ruleRepository;
    @Mock private RiskAssessmentRepository riskAssessmentRepository;
    @Mock private RiskAssessmentService riskAssessmentService;
    @Mock private ExamAnalysisService examAnalysisService;
    @Mock private ExamIResultMapper examIResultMapper;
    @Mock private ExamAnalysisResultMapper examAnalysisResultMapper;
    @Mock private ExamNotificationService examNotificationService;
    @Mock private AchievementEvaluator achievementEvaluator;

    private ExamService service;
    private AppUser user;
    private HealthUnit healthUnit;
    private Exam existing;

    @BeforeEach
    void setUp() {
        // mapper real, para o mapeamento de fato ser exercitado
        service = new ExamService(examRepository, appUserRepository, healthUnitRepository,
                userServiceHelper, examItemRepository, Mappers.getMapper(ExamMapper.class),
                examResultRepository, referenceRangeRepository, ruleRepository,
                riskAssessmentRepository, riskAssessmentService, examAnalysisService,
                achievementEvaluator, examIResultMapper, examAnalysisResultMapper,
                examNotificationService, Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));

        user = new AppUser();
        user.setId(USER_ID);

        healthUnit = new HealthUnit();
        healthUnit.setId(HEALTH_UNIT_ID);

        existing = new Exam();
        existing.setId(EXAM_ID);
        existing.setCollectedAt(COLLECTED_AT);
        existing.setStatus(ExamStatus.COLLECTED);
        existing.setExternalReference("LAB_ORIGINAL");
        existing.setUser(user);
        existing.setHealthUnit(healthUnit);
    }

    private ExamRecoverDTO dto(final Long userId, final Long healthUnitId) {
        final ExamRecoverDTO d = new ExamRecoverDTO();
        d.setCollectedAt(NOW);
        d.setStatus(ExamStatus.RELEASED);
        d.setExternalReference("LAB_NOVO");
        d.setUserId(userId);
        d.setHealthUnitId(healthUnitId);
        return d;
    }

    @Test
    @DisplayName("update altera o exame do path, e nao insere um exame novo")
    void shouldMutateTheExamFromThePath() {
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(existing));
        given(userServiceHelper.getUserByIdOrFail(USER_ID)).willReturn(user);
        given(healthUnitRepository.findById(HEALTH_UNIT_ID)).willReturn(Optional.of(healthUnit));

        service.update(EXAM_ID, dto(USER_ID, HEALTH_UNIT_ID));

        final ArgumentCaptor<Exam> captor = ArgumentCaptor.forClass(Exam.class);
        then(examRepository).should().save(captor.capture());
        final Exam saved = captor.getValue();

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getId()).isEqualTo(EXAM_ID);
        assertThat(saved.getExternalReference()).isEqualTo("LAB_NOVO");
        assertThat(saved.getStatus()).isEqualTo(ExamStatus.RELEASED);
        assertThat(saved.getCollectedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("update preserva os vinculos com cidadao e unidade de saude")
    void shouldKeepRelations() {
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(existing));
        given(userServiceHelper.getUserByIdOrFail(USER_ID)).willReturn(user);
        given(healthUnitRepository.findById(HEALTH_UNIT_ID)).willReturn(Optional.of(healthUnit));

        service.update(EXAM_ID, dto(USER_ID, HEALTH_UNIT_ID));

        assertThat(existing.getUser()).isSameAs(user);
        assertThat(existing.getHealthUnit()).isSameAs(healthUnit);
    }

    @Test
    @DisplayName("update sem os ids no corpo nao apaga os vinculos que o exame ja tem")
    void shouldNotClearRelationsOnPartialUpdate() {
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(existing));

        service.update(EXAM_ID, dto(null, null));

        assertThat(existing.getUser()).isSameAs(user);
        assertThat(existing.getHealthUnit()).isSameAs(healthUnit);
        then(examRepository).should().save(existing);
    }

    @Test
    @DisplayName("update de exame inexistente falha com 404 e nao salva nada")
    void shouldFailWhenExamDoesNotExist() {
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(EXAM_ID, dto(USER_ID, HEALTH_UNIT_ID)))
                .isInstanceOf(NotFoundException.class);

        then(examRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("update com unidade de saude inexistente falha com 404 e nao salva nada")
    void shouldFailWhenHealthUnitDoesNotExist() {
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(existing));
        given(userServiceHelper.getUserByIdOrFail(USER_ID)).willReturn(user);
        given(healthUnitRepository.findById(HEALTH_UNIT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(EXAM_ID, dto(USER_ID, HEALTH_UNIT_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(String.valueOf(HEALTH_UNIT_ID));

        then(examRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }
}
