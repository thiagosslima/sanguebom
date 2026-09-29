package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.doctor.ComparedExamDTO;
import br.com.fiap.sanguebom.model.doctor.ComparedExamItemDTO;
import br.com.fiap.sanguebom.model.doctor.ComparedExamValueDTO;
import br.com.fiap.sanguebom.model.doctor.ExamComparisonDTO;
import br.com.fiap.sanguebom.model.doctor.ExamItemVariationDTO;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoctorExamComparisonService {

    private static final int MIN_EXAMS_TO_COMPARE = 2;
    private static final int PERCENT_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final AppUserRepository appUserRepository;
    private final ExamRepository examRepository;
    private final MessageService messageService;

    public DoctorExamComparisonService(final AppUserRepository appUserRepository,
            final ExamRepository examRepository,
            final MessageService messageService) {
        this.appUserRepository = appUserRepository;
        this.examRepository = examRepository;
        this.messageService = messageService;
    }

    @Transactional(readOnly = true)
    public ExamComparisonDTO compare(final Long userId, final List<Long> examIds) {
        validateExamIds(examIds);

        appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        messageService.getMessage(ApplicationMessage.DOCTOR_EXAM_COMPARISON_PATIENT_NOT_FOUND,
                                userId)));

        final List<Exam> exams = examRepository.findByUserIdAndIdInWithResults(userId, examIds);
        if (exams.size() != examIds.size()) {
            throw new NotFoundException(
                    messageService.getMessage(ApplicationMessage.DOCTOR_EXAM_COMPARISON_EXAM_NOT_FOUND,
                            userId));
        }

        final List<Exam> orderedExams = exams.stream()
                .sorted(Comparator.comparing(Exam::getCollectedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Exam::getId))
                .toList();

        final Map<Long, Exam> examsById = orderedExams.stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));

        final List<ComparedExamDTO> comparedExams = orderedExams.stream()
                .map(DoctorExamComparisonService::toComparedExam)
                .toList();

        return new ExamComparisonDTO(userId, comparedExams, comparedItems(orderedExams, examsById));
    }

    private void validateExamIds(final List<Long> examIds) {
        if (examIds == null || examIds.size() < MIN_EXAMS_TO_COMPARE) {
            throw badRequest(ApplicationMessage.DOCTOR_EXAM_COMPARISON_INSUFFICIENT_EXAMS);
        }

        final Set<Long> uniqueExamIds = new HashSet<>(examIds);
        if (uniqueExamIds.size() != examIds.size()) {
            throw badRequest(ApplicationMessage.DOCTOR_EXAM_COMPARISON_DUPLICATED_EXAMS);
        }
    }

    private ResponseStatusException badRequest(final ApplicationMessage applicationMessage) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                messageService.getMessage(applicationMessage));
    }

    private static ComparedExamDTO toComparedExam(final Exam exam) {
        return new ComparedExamDTO(exam.getId(), exam.getCollectedAt(), exam.getReleasedAt(), exam.getStatus());
    }

    private List<ComparedExamItemDTO> comparedItems(final List<Exam> orderedExams, final Map<Long, Exam> examsById) {
        final Map<String, ItemComparisonGroup> groups = new LinkedHashMap<>();

        orderedExams.forEach(exam -> exam.getExamResults().stream()
                .filter(result -> result.getExamItem() != null && result.getExamItem().getCode() != null)
                .sorted(Comparator.comparing(result -> result.getExamItem().getCode()))
                .forEach(result -> addResult(groups, exam, result)));

        return groups.values().stream()
                .map(group -> toComparedItem(group, orderedExams, examsById))
                .toList();
    }

    private static void addResult(final Map<String, ItemComparisonGroup> groups,
            final Exam exam, final ExamResult result) {
        final ExamItem item = result.getExamItem();
        final ItemComparisonGroup group = groups.computeIfAbsent(item.getCode(),
                ignored -> new ItemComparisonGroup(item.getCode(), item.getName(), item.getUnit()));
        group.resultsByExamId().put(exam.getId(), result);
    }

    private ComparedExamItemDTO toComparedItem(final ItemComparisonGroup group, final List<Exam> orderedExams,
            final Map<Long, Exam> examsById) {
        final List<ComparedExamValueDTO> values = orderedExams.stream()
                .map(exam -> toComparedValue(exam.getId(), group.resultsByExamId().get(exam.getId()), group.unit()))
                .toList();

        return new ComparedExamItemDTO(
                group.itemCode(),
                group.itemName(),
                group.unit(),
                values,
                variations(group, orderedExams, examsById));
    }

    private static ComparedExamValueDTO toComparedValue(final Long examId, final ExamResult result,
            final String defaultUnit) {
        if (result == null) {
            return new ComparedExamValueDTO(examId, null, null, defaultUnit, null);
        }

        return new ComparedExamValueDTO(
                examId,
                result.getValueNumeric(),
                result.getValueText(),
                result.getUnit() == null ? defaultUnit : result.getUnit(),
                result.getFlag());
    }

    private List<ExamItemVariationDTO> variations(final ItemComparisonGroup group, final List<Exam> orderedExams,
            final Map<Long, Exam> examsById) {
        final List<ExamItemVariationDTO> variations = new ArrayList<>();

        for (int index = 1; index < orderedExams.size(); index++) {
            final Exam previousExam = orderedExams.get(index - 1);
            final Exam currentExam = orderedExams.get(index);
            final BigDecimal previousValue = numericValue(group, previousExam.getId());
            final BigDecimal currentValue = numericValue(group, currentExam.getId());
            final BigDecimal absoluteVariation = absoluteVariation(previousValue, currentValue);

            variations.add(new ExamItemVariationDTO(
                    examsById.get(previousExam.getId()).getId(),
                    examsById.get(currentExam.getId()).getId(),
                    absoluteVariation,
                    percentVariation(previousValue, absoluteVariation)));
        }

        return variations;
    }

    private static BigDecimal numericValue(final ItemComparisonGroup group, final Long examId) {
        return Objects.requireNonNullElse(group.resultsByExamId().get(examId), new ExamResult()).getValueNumeric();
    }

    private static BigDecimal absoluteVariation(final BigDecimal previousValue, final BigDecimal currentValue) {
        if (previousValue == null || currentValue == null) {
            return null;
        }
        return currentValue.subtract(previousValue);
    }

    private static BigDecimal percentVariation(final BigDecimal previousValue, final BigDecimal absoluteVariation) {
        if (previousValue == null || BigDecimal.ZERO.compareTo(previousValue) == 0 || absoluteVariation == null) {
            return null;
        }
        return absoluteVariation
                .multiply(ONE_HUNDRED)
                .divide(previousValue, PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private record ItemComparisonGroup(
            String itemCode,
            String itemName,
            String unit,
            Map<Long, ExamResult> resultsByExamId
    ) {
        private ItemComparisonGroup(final String itemCode, final String itemName, final String unit) {
            this(itemCode, itemName, unit, new LinkedHashMap<>());
        }
    }
}
