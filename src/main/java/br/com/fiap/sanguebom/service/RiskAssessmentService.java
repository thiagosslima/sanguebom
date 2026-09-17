package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.dtos.RiskAssessmentDTO;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisScore;
import br.com.fiap.sanguebom.model.riskAssessment.RiskClassification;
import br.com.fiap.sanguebom.model.riskAssessment.RiskTimelinePointDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.RiskAssessmentRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.rulesMotor.RiskAssessmentClassifier;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private static final int NEXT_DAY_OFFSET = 1;

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AppUserRepository appUserRepository;
    private final ExamRepository examRepository;
    private final RiskAssessmentClassifier riskAssessmentClassifier;

    public RiskAssessment createRiskAssessment(
            Exam exam,
            AppUser user,
            ExamAnalysisScore analysisResult
    ) {

        RiskClassification classification =
                riskAssessmentClassifier.classify(
                        analysisResult.finalScore(),
                        Locale.getDefault()
                );

        RiskAssessment riskAssessment =
                new RiskAssessment();

        riskAssessment.setExam(exam);
        riskAssessment.setUser(user);

        riskAssessment.applyAssessment(
                analysisResult.finalScore(),
                classification
        );

        return riskAssessment;
    }

    public List<RiskAssessmentDTO> findAll() {
        final List<RiskAssessment> riskAssessments = riskAssessmentRepository.findAll(Sort.by("id"));
        return riskAssessments.stream()
                .map(riskAssessment -> mapToDTO(riskAssessment, new RiskAssessmentDTO()))
                .toList();
    }

    public RiskAssessmentDTO get(final Long id) {
        return riskAssessmentRepository.findById(id)
                .map(riskAssessment -> mapToDTO(riskAssessment, new RiskAssessmentDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public PageResponse<RiskTimelinePointDTO> findByUserId(final Long userId, final LocalDate from, final LocalDate to, final int page, final int size) {
        validatePeriod(from, to);

        // TODO: Implementar ApplicationMessages depois do merge da PR #12
        appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado: {0}".formatted(userId)));

        final OffsetDateTime fromAt = startOfDay(from);
        final OffsetDateTime toExclusive = startOfNextDay(to);
        final PageRequest pageRequest = PageRequest.of(page, size);

        final Page<RiskTimelinePointDTO> points =
                findTimelineResults(userId, fromAt, toExclusive, pageRequest)
                        .map(this::toTimelinePoint);

        return PageResponse.of(points);
    }

    private Page<RiskAssessment> findTimelineResults(final Long userId, final OffsetDateTime fromAt,
                                                     final OffsetDateTime toExclusive, final PageRequest pageRequest) {
        Specification<RiskAssessment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (fromAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromAt));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return riskAssessmentRepository.findAll(spec, pageRequest);
    }

    private RiskTimelinePointDTO toTimelinePoint(final RiskAssessment result) {
        return new RiskTimelinePointDTO(
                result.getId(),
                result.getExam().getId(),
                result.getScore(),
                result.getLevel(),
                result.getCreatedAt()
        );
    }

    public void update(final Long id, final RiskAssessmentDTO riskAssessmentDTO) {
        final RiskAssessment riskAssessment = riskAssessmentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(riskAssessmentDTO, riskAssessment);
        riskAssessmentRepository.save(riskAssessment);
    }

    private RiskAssessmentDTO mapToDTO(final RiskAssessment riskAssessment,
                                       final RiskAssessmentDTO riskAssessmentDTO) {
        riskAssessmentDTO.setId(riskAssessment.getId());
        riskAssessmentDTO.setScore(riskAssessment.getScore());
        riskAssessmentDTO.setLevel(riskAssessment.getLevel());
        riskAssessmentDTO.setRulesVersion(riskAssessment.getRulesVersion());
        riskAssessmentDTO.setExplanation(riskAssessment.getExplanation());
        riskAssessmentDTO.setCreatedAt(riskAssessment.getCreatedAt());
        riskAssessmentDTO.setUser(riskAssessment.getUser() == null ? null : riskAssessment.getUser().getId());
        riskAssessmentDTO.setExam(riskAssessment.getExam() == null ? null : riskAssessment.getExam().getId());
        return riskAssessmentDTO;
    }

    private RiskAssessment mapToEntity(final RiskAssessmentDTO riskAssessmentDTO,
                                       final RiskAssessment riskAssessment) {
        riskAssessment.setScore(riskAssessmentDTO.getScore());
        riskAssessment.setLevel(riskAssessmentDTO.getLevel());
        riskAssessment.setRulesVersion(riskAssessmentDTO.getRulesVersion());
        riskAssessment.setExplanation(riskAssessmentDTO.getExplanation());
        riskAssessment.setCreatedAt(riskAssessmentDTO.getCreatedAt());
        final AppUser user = riskAssessmentDTO.getUser() == null ? null : appUserRepository.findById(riskAssessmentDTO.getUser())
                .orElseThrow(() -> new NotFoundException("user not found"));
        riskAssessment.setUser(user);
        final Exam exam = riskAssessmentDTO.getExam() == null ? null : examRepository.findById(riskAssessmentDTO.getExam())
                .orElseThrow(() -> new NotFoundException("exam not found"));
        riskAssessment.setExam(exam);
        return riskAssessment;
    }

    private static OffsetDateTime startOfDay(final LocalDate date) {
        // TODO: Colocar em uma classe útil para reuso em outros métodos
        return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime startOfNextDay(final LocalDate date) {
        // TODO: Colocar em uma classe útil para reuso em outros métodos
        return date == null ? null : startOfDay(date.plusDays(NEXT_DAY_OFFSET));
    }

    private void validatePeriod(final LocalDate from, final LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            // TODO: Colocar em uma classe útil para reuso em outros métodos
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período inicial não pode ser maior que o período final.");
        }
    }
}
