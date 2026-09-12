package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.doctor.MarkerTimelinePointDTO;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.entities.Rule;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DoctorTimelineServiceTest {

    private static final Long USER_ID = 1L;
    private static final String ITEM_CODE = "GLI_JEJUM";
    private static final OffsetDateTime COLLECTED_AT =
            OffsetDateTime.of(2026, 8, 15, 8, 30, 0, 0, ZoneOffset.UTC);

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ExamItemRepository examItemRepository;

    @Mock
    private ExamResultRepository examResultRepository;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private DoctorTimelineService service;

    @BeforeEach
    void setUp() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        final Locale locale = Locale.getDefault();
        messageSource.addMessage("doctor.timeline.patient-not-found", locale,
                "Paciente não encontrado: {0}");
        messageSource.addMessage("doctor.timeline.exam-item-not-found", locale,
                "Marcador ativo não encontrado para o código: {0}");
        messageSource.addMessage("doctor.timeline.invalid-period", locale,
                "O período inicial não pode ser maior que o período final");
        service = new DoctorTimelineService(appUserRepository, examItemRepository,
                examResultRepository, referenceRangeRepository, new MessageService(messageSource));
    }

    @Test
    @DisplayName("timeline retorna pagina de pontos com valor, unidade, classificacao e faixa vigente")
    void shouldReturnPagedTimelineWithReferenceRange() {
        final AppUser user = user();
        final ExamItem item = item();
        final ExamResult result = result(item, null, ExamResultFlag.ATTENTION);
        final ReferenceRange range = referenceRange(rule());

        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(examItemRepository.findActiveByCode(ITEM_CODE)).willReturn(Optional.of(item));
        given(examResultRepository.findTimelineByUserAndItemCode(eq(USER_ID), eq(ITEM_CODE),
                any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(result)));
        given(referenceRangeRepository.findApplicableRangeForTimeline(eq(item.getId()), eq(Sex.MALE),
                eq(new BigDecimal("46")), eq(LocalDate.of(2026, 8, 15))))
                .willReturn(List.of(range));

        final PageResponse<MarkerTimelinePointDTO> response = service.timeline(USER_ID, ITEM_CODE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0, 10);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).singleElement().satisfies(point -> {
            assertThat(point.examId()).isEqualTo(10L);
            assertThat(point.collectedAt()).isEqualTo(COLLECTED_AT);
            assertThat(point.valueNumeric()).isEqualByComparingTo("115.00");
            assertThat(point.unit()).isEqualTo("mg/dL");
            assertThat(point.flag()).isEqualTo(ExamResultFlag.ATTENTION);
            assertThat(point.referenceRange().version()).isEqualTo("SBD_2023");
            assertThat(point.matchedRule().level()).isEqualTo(ExamResultFlag.ATTENTION);
        });
    }

    @Test
    @DisplayName("timeline pagina no repositorio usando page e size informados")
    void shouldDelegatePaginationToRepository() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(examItemRepository.findActiveByCode(ITEM_CODE)).willReturn(Optional.of(item()));
        given(examResultRepository.findTimelineByUserAndItemCode(anyLong(), eq(ITEM_CODE),
                any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.timeline(USER_ID, ITEM_CODE, null, null, 2, 5);

        then(examResultRepository).should().findTimelineByUserAndItemCode(eq(USER_ID), eq(ITEM_CODE),
                eq(null), eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("periodo inicial maior que final retorna erro de requisicao")
    void shouldRejectInvalidPeriod() {
        assertThatThrownBy(() -> service.timeline(USER_ID, ITEM_CODE,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), 0, 10))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    @DisplayName("paciente inexistente retorna not found")
    void shouldRejectUnknownPatient() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.timeline(USER_ID, ITEM_CODE, null, null, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Paciente não encontrado");
    }

    @Test
    @DisplayName("marcador inexistente retorna not found")
    void shouldRejectUnknownExamItem() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(examItemRepository.findActiveByCode(ITEM_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.timeline(USER_ID, ITEM_CODE, null, null, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Marcador ativo não encontrado");
    }

    private static AppUser user() {
        final AppUser user = new AppUser();
        user.setId(USER_ID);
        user.setBirthDate(LocalDate.of(1980, 4, 10));

        final HealthProfile healthProfile = new HealthProfile();
        healthProfile.setSex(Sex.MALE);
        healthProfile.setUser(user);
        user.setHealthProfile(healthProfile);
        return user;
    }

    private static ExamItem item() {
        final ExamItem item = new ExamItem();
        item.setId(1L);
        item.setCode(ITEM_CODE);
        item.setName("Glicemia em Jejum");
        item.setUnit("mg/dL");
        item.setActive(true);
        return item;
    }

    private static ExamResult result(final ExamItem item, final String unit, final ExamResultFlag flag) {
        final Exam exam = new Exam();
        exam.setId(10L);
        exam.setCollectedAt(COLLECTED_AT);

        final ExamResult result = new ExamResult();
        result.setId(100L);
        result.setExam(exam);
        result.setExamItem(item);
        result.setValueNumeric(new BigDecimal("115.00"));
        result.setUnit(unit);
        result.setFlag(flag);
        return result;
    }

    private static ReferenceRange referenceRange(final Rule rule) {
        final ReferenceRange range = new ReferenceRange();
        range.setId(20L);
        range.setVersion("SBD_2023");
        range.setSource("Sociedade Brasileira de Diabetes");
        range.setValidFrom(LocalDate.of(2023, 1, 1));
        range.getReferenceRangeRules().add(rule);
        rule.setReferenceRange(range);
        return range;
    }

    private static Rule rule() {
        final Rule rule = new Rule();
        rule.setId(30L);
        rule.setMinValue(new BigDecimal("100.00"));
        rule.setMaxValue(new BigDecimal("125.00"));
        rule.setMinInclusive(true);
        rule.setMaxInclusive(true);
        rule.setLevel(ExamResultFlag.ATTENTION);
        rule.setDescription("Glicemia de jejum alterada");
        rule.setVersion("1.0");
        rule.setActive(true);
        return rule;
    }
}
