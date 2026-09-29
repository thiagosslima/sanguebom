package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.doctor.ExamComparisonDTO;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DoctorExamComparisonServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ExamRepository examRepository;

    private DoctorExamComparisonService service;

    @BeforeEach
    void setUp() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        final Locale locale = Locale.getDefault();
        messageSource.addMessage("doctor.exam-comparison.patient-not-found", locale,
                "Paciente não encontrado: {0}");
        messageSource.addMessage("doctor.exam-comparison.insufficient-exams", locale,
                "Informe pelo menos dois exames para comparação");
        messageSource.addMessage("doctor.exam-comparison.exam-not-found", locale,
                "Um ou mais exames não foram encontrados para o paciente: {0}");
        messageSource.addMessage("doctor.exam-comparison.duplicated-exams", locale,
                "Não informe exames duplicados para comparação");

        service = new DoctorExamComparisonService(appUserRepository, examRepository,
                new MessageService(messageSource));
    }

    @Test
    @DisplayName("compara exames por marcador calculando variacao entre exames consecutivos")
    void shouldCompareExamItemsWithSequentialVariations() {
        final ExamItem glucose = item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL");
        final ExamItem hba1c = item("HBA1C", "Hemoglobina glicada", "%");
        final Exam first = exam(10L, 2024, 88, glucose);
        final Exam second = exam(20L, 2025, 110, glucose);
        final Exam third = exam(30L, 2026, 132, glucose);
        third.getExamResults().add(result(third, hba1c, new BigDecimal("6.10"), "%", ExamResultFlag.ATTENTION));

        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(examRepository.findByUserIdAndIdInWithResults(USER_ID, List.of(30L, 10L, 20L)))
                .willReturn(List.of(third, first, second));

        final ExamComparisonDTO comparison = service.compare(USER_ID, List.of(30L, 10L, 20L));

        assertThat(comparison.userId()).isEqualTo(USER_ID);
        assertThat(comparison.exams())
                .extracting("examId")
                .containsExactly(10L, 20L, 30L);
        assertThat(comparison.items()).hasSize(2);
        assertThat(comparison.items().getFirst().itemCode()).isEqualTo("GLI_JEJUM");
        assertThat(comparison.items().getFirst().values())
                .extracting("examId")
                .containsExactly(10L, 20L, 30L);
        assertThat(comparison.items().getFirst().variations()).satisfiesExactly(
                variation -> {
                    assertThat(variation.fromExamId()).isEqualTo(10L);
                    assertThat(variation.toExamId()).isEqualTo(20L);
                    assertThat(variation.absoluteVariation()).isEqualByComparingTo("22");
                    assertThat(variation.percentVariation()).isEqualByComparingTo("25.00");
                },
                variation -> {
                    assertThat(variation.fromExamId()).isEqualTo(20L);
                    assertThat(variation.toExamId()).isEqualTo(30L);
                    assertThat(variation.absoluteVariation()).isEqualByComparingTo("22");
                    assertThat(variation.percentVariation()).isEqualByComparingTo("20.00");
                });
        assertThat(comparison.items().get(1).values().getFirst().valueNumeric()).isNull();
        assertThat(comparison.items().get(1).variations().getFirst().absoluteVariation()).isNull();
    }

    @Test
    @DisplayName("percentual fica nulo quando valor anterior e zero")
    void shouldReturnNullPercentVariationWhenPreviousValueIsZero() {
        final ExamItem glucose = item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL");
        final Exam first = exam(10L, 2024, 0, glucose);
        final Exam second = exam(20L, 2025, 10, glucose);

        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(examRepository.findByUserIdAndIdInWithResults(USER_ID, List.of(10L, 20L)))
                .willReturn(List.of(first, second));

        final ExamComparisonDTO comparison = service.compare(USER_ID, List.of(10L, 20L));

        assertThat(comparison.items()).singleElement().satisfies(item -> {
            assertThat(item.variations()).singleElement().satisfies(variation -> {
                assertThat(variation.absoluteVariation()).isEqualByComparingTo("10");
                assertThat(variation.percentVariation()).isNull();
            });
        });
    }

    @Test
    @DisplayName("menos de dois exames retorna bad request")
    void shouldRejectInsufficientExams() {
        assertThatThrownBy(() -> service.compare(USER_ID, List.of(10L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    @DisplayName("exames duplicados retornam bad request")
    void shouldRejectDuplicatedExams() {
        assertThatThrownBy(() -> service.compare(USER_ID, List.of(10L, 10L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    @DisplayName("exame inexistente para o paciente retorna not found")
    void shouldRejectExamFromAnotherPatient() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(examRepository.findByUserIdAndIdInWithResults(USER_ID, List.of(10L, 20L)))
                .willReturn(List.of(exam(10L, 2024, 88, item("GLI_JEJUM", "Glicemia em Jejum", "mg/dL"))));

        assertThatThrownBy(() -> service.compare(USER_ID, List.of(10L, 20L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Um ou mais exames não foram encontrados");
    }

    private static AppUser user() {
        final AppUser user = new AppUser();
        user.setId(USER_ID);
        return user;
    }

    private static Exam exam(final Long id, final int year, final int value, final ExamItem item) {
        final Exam exam = new Exam();
        exam.setId(id);
        exam.setCollectedAt(OffsetDateTime.of(year, 8, 10, 8, 0, 0, 0, ZoneOffset.UTC));
        exam.setReleasedAt(exam.getCollectedAt().plusDays(1));
        exam.setStatus(ExamStatus.RELEASED);
        exam.getExamResults().add(result(exam, item, BigDecimal.valueOf(value), null, ExamResultFlag.NORMAL));
        return exam;
    }

    private static ExamItem item(final String code, final String name, final String unit) {
        final ExamItem item = new ExamItem();
        item.setCode(code);
        item.setName(name);
        item.setUnit(unit);
        return item;
    }

    private static ExamResult result(final Exam exam, final ExamItem item, final BigDecimal value,
            final String unit, final ExamResultFlag flag) {
        final ExamResult result = new ExamResult();
        result.setExam(exam);
        result.setExamItem(item);
        result.setValueNumeric(value);
        result.setUnit(unit);
        result.setFlag(flag);
        return result;
    }
}
