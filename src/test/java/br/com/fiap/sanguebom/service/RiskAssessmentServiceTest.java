package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.RiskAssessmentDTO;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisScore;
import br.com.fiap.sanguebom.model.riskAssessment.RiskClassification;
import br.com.fiap.sanguebom.model.riskAssessment.RiskTimelinePointDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.RiskAssessmentRepository;
import br.com.fiap.sanguebom.rulesMotor.RiskAssessmentClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RiskAssessmentRepository repository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private RiskAssessmentClassifier classifier;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private RiskAssessmentService service;

    @BeforeEach
    void setUp() {
        service = new RiskAssessmentService(repository, appUserRepository, examRepository, classifier, messageService);
    }

    @Test
    void shouldCreateRiskAssessment() {
        Exam exam = new Exam();
        AppUser user = new AppUser();
        ExamAnalysisScore analysisResult = mock(ExamAnalysisScore.class);
        RiskClassification classification = mock(RiskClassification.class);
        when(analysisResult.finalScore()).thenReturn(BigDecimal.valueOf(75.0));
        when(classifier.classify(BigDecimal.valueOf(75.0), java.util.Locale.getDefault())).thenReturn(classification);
        RiskAssessment result = service.createRiskAssessment(exam, user, analysisResult);
        assertThat(result).isNotNull();
        assertThat(result.getExam()).isSameAs(exam);
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getScore()).isEqualTo(BigDecimal.valueOf(75.0));
        verify(classifier).classify(BigDecimal.valueOf(75.0), java.util.Locale.getDefault());
    }

    @Test
    void shouldFindAllRiskAssessments() {
        RiskAssessment riskAssessment1 = createRiskAssessment(1L, 80.0, null);
        RiskAssessment riskAssessment2 = createRiskAssessment(2L, 40.0, null);
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(riskAssessment1, riskAssessment2));
        List<RiskAssessmentDTO> result = service.findAll();
        assertThat(result).hasSize(2).extracting(RiskAssessmentDTO::getId).containsExactly(1L, 2L);
        assertThat(result.get(0).getScore()).isEqualTo(BigDecimal.valueOf(80.0));
        assertThat(result.get(1).getScore()).isEqualTo(BigDecimal.valueOf(40.0));
        verify(repository).findAll(Sort.by("id"));
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoRiskAssessments() {
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of());
        List<RiskAssessmentDTO> result = service.findAll();
        assertThat(result).isEmpty();
        verify(repository).findAll(Sort.by("id"));
    }

    @Test
    void shouldGetRiskAssessmentById() {
        RiskAssessment riskAssessment = createRiskAssessment(1L, 85.0, null);
        when(repository.findById(1L)).thenReturn(Optional.of(riskAssessment));
        RiskAssessmentDTO result = service.get(1L);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getScore()).isEqualTo(BigDecimal.valueOf(85.0));
        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenRiskAssessmentDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(999L)).isInstanceOf(NotFoundException.class);
        verify(repository).findById(999L);
    }

    @Test
    void shouldFindRiskAssessmentTimelineByUserId() {
        Long userId = 10L;
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 10);
        Exam exam = new Exam();
        exam.setId(20L);
        RiskAssessment riskAssessment = createRiskAssessment(1L, 75.0, exam);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-05T14:30:00Z");
        riskAssessment.setCreatedAt(createdAt);
        Page<RiskAssessment> page = new PageImpl<>(List.of(riskAssessment));
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(new AppUser()));
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class))).thenReturn(page);
        PageResponse<RiskTimelinePointDTO> result = service.findByUserId(userId, from, to, 0, 10);
        assertThat(result).isNotNull();
        verify(appUserRepository).findById(userId);
        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenTimelineUserDoesNotExist() {
        Long userId = 999L;
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());
        when(messageService.getMessage(ApplicationMessage.RISK_ASSESSMENT_PATIENT_NOT_FOUND, userId)).thenReturn("Patient not found");
        assertThatThrownBy(() -> service.findByUserId(userId,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), 0, 10))
                .isInstanceOf(NotFoundException.class);
        verify(appUserRepository).findById(userId);
        verify(repository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }

    @Test
    void shouldThrowBadRequestWhenPeriodIsInvalid() {
        LocalDate from = LocalDate.of(2026, 9, 10);
        LocalDate to = LocalDate.of(2026, 9, 1);
        when(messageService.getMessage(ApplicationMessage.RISK_ASSESSMENT_INVALID_PERIOD)).thenReturn("Invalid period");
        assertThatThrownBy(() -> service.findByUserId(1L, from, to, 0, 10))
                .isInstanceOf(ResponseStatusException.class).satisfies(exception -> {
                    ResponseStatusException responseException = (ResponseStatusException) exception;
                    assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verifyNoInteractions(appUserRepository);
        verifyNoInteractions(repository);
    }

    @Test
    void shouldUpdateRiskAssessment() {
        Long riskAssessmentId = 1L;
        AppUser user = new AppUser();
        user.setId(10L);
        Exam exam = new Exam();
        exam.setId(20L);
        RiskAssessment riskAssessment = createRiskAssessment(1L, 50.0, null);
        RiskAssessmentDTO dto = new RiskAssessmentDTO();
        dto.setScore(BigDecimal.valueOf(90.0));
        dto.setRulesVersion("2.0");
        dto.setExplanation("Updated assessment");
        dto.setCreatedAt(OffsetDateTime.parse("2026-09-17T10:00:00Z"));
        dto.setUser(10L);
        dto.setExam(20L);
        when(repository.findById(riskAssessmentId)).thenReturn(Optional.of(riskAssessment));
        when(appUserRepository.findById(10L)).thenReturn(Optional.of(user));
        when(examRepository.findById(20L)).thenReturn(Optional.of(exam));
        service.update(riskAssessmentId, dto);
        assertThat(riskAssessment.getScore()).isEqualTo(BigDecimal.valueOf(90.0));
        assertThat(riskAssessment.getRulesVersion()).isEqualTo("2.0");
        assertThat(riskAssessment.getExplanation()).isEqualTo("Updated assessment");
        assertThat(riskAssessment.getCreatedAt()).isEqualTo(dto.getCreatedAt());
        assertThat(riskAssessment.getUser()).isSameAs(user);
        assertThat(riskAssessment.getExam()).isSameAs(exam);
        verify(repository).save(riskAssessment);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistingRiskAssessment() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(999L, new RiskAssessmentDTO())).isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExistDuringUpdate() {
        RiskAssessment riskAssessment = createRiskAssessment(1L, 50.0, null);
        RiskAssessmentDTO dto = new RiskAssessmentDTO();
        dto.setUser(999L);
        when(repository.findById(1L)).thenReturn(Optional.of(riskAssessment));
        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, dto)).isInstanceOf(NotFoundException.class).hasMessage("user not found");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenExamDoesNotExistDuringUpdate() {
        RiskAssessment riskAssessment = createRiskAssessment(1L, 50.0, null);
        RiskAssessmentDTO dto = new RiskAssessmentDTO();
        dto.setExam(999L);
        when(repository.findById(1L)).thenReturn(Optional.of(riskAssessment));
        when(examRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, dto)).isInstanceOf(NotFoundException.class).hasMessage("exam not found");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateRiskAssessmentWithoutUserAndExam() {
        RiskAssessment riskAssessment = createRiskAssessment(1L, 50.0, null);
        RiskAssessmentDTO dto = new RiskAssessmentDTO();
        dto.setScore(BigDecimal.valueOf(80.0));
        dto.setRulesVersion("2.0");
        dto.setExplanation("Updated");
        when(repository.findById(1L)).thenReturn(Optional.of(riskAssessment));
        service.update(1L, dto);
        assertThat(riskAssessment.getScore()).isEqualTo(BigDecimal.valueOf(80.0));
        assertThat(riskAssessment.getRulesVersion()).isEqualTo("2.0");
        assertThat(riskAssessment.getExplanation()).isEqualTo("Updated");
        assertThat(riskAssessment.getUser()).isNull();
        assertThat(riskAssessment.getExam()).isNull();
        verify(appUserRepository, never()).findById(any());
        verify(examRepository, never()).findById(any());
        verify(repository).save(riskAssessment);
    }

    private RiskAssessment createRiskAssessment(Long id, Double score, Exam exam) {
        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setId(id);
        riskAssessment.setScore(BigDecimal.valueOf(score));
        riskAssessment.setExam(exam);
        return riskAssessment;
    }
}