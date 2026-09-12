package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.doctor.MarkerTimelinePointDTO;
import br.com.fiap.sanguebom.model.doctor.ReferenceRangeSnapshotDTO;
import br.com.fiap.sanguebom.model.doctor.ReferenceRuleSnapshotDTO;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.entities.Rule;
import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
import br.com.fiap.sanguebom.model.enums.Sex;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;

@Service
public class DoctorTimelineService {

    private static final int NEXT_DAY_OFFSET = 1;

    private final AppUserRepository appUserRepository;
    private final ExamItemRepository examItemRepository;
    private final ExamResultRepository examResultRepository;
    private final ReferenceRangeRepository referenceRangeRepository;
    private final MessageService messageService;

    public DoctorTimelineService(final AppUserRepository appUserRepository,
            final ExamItemRepository examItemRepository,
            final ExamResultRepository examResultRepository,
            final ReferenceRangeRepository referenceRangeRepository,
            final MessageService messageService) {
        this.appUserRepository = appUserRepository;
        this.examItemRepository = examItemRepository;
        this.examResultRepository = examResultRepository;
        this.referenceRangeRepository = referenceRangeRepository;
        this.messageService = messageService;
    }

    @Transactional(readOnly = true)
    public PageResponse<MarkerTimelinePointDTO> timeline(final Long userId, final String itemCode,
            final LocalDate from, final LocalDate to, final int page, final int size) {
        validatePeriod(from, to);

        final AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        messageService.getMessage(ApplicationMessage.DOCTOR_TIMELINE_PATIENT_NOT_FOUND,
                                userId)));

        examItemRepository.findActiveByCode(itemCode)
                .orElseThrow(() -> new NotFoundException(
                        messageService.getMessage(ApplicationMessage.DOCTOR_TIMELINE_EXAM_ITEM_NOT_FOUND,
                                itemCode)));

        final OffsetDateTime fromAt = startOfDay(from);
        final OffsetDateTime toExclusive = startOfNextDay(to);

        final Page<MarkerTimelinePointDTO> points = examResultRepository
                .findTimelineByUserAndItemCode(userId, itemCode, fromAt, toExclusive,
                        PageRequest.of(page, size))
                .map(result -> toTimelinePoint(result, user));

        return PageResponse.of(points);
    }

    private MarkerTimelinePointDTO toTimelinePoint(final ExamResult result, final AppUser user) {
        final LocalDate collectedDate = result.getExam().getCollectedAt().toLocalDate();
        final ReferenceRange referenceRange = findReferenceRange(result, user, collectedDate).orElse(null);
        final Rule matchedRule = findMatchedRule(referenceRange, result).orElse(null);

        return new MarkerTimelinePointDTO(
                result.getExam().getId(),
                result.getExam().getCollectedAt(),
                result.getValueNumeric(),
                result.getValueText(),
                result.getUnit() == null ? result.getExamItem().getUnit() : result.getUnit(),
                result.getFlag(),
                referenceRange == null ? null : toReferenceRangeSnapshot(referenceRange),
                matchedRule == null ? null : toReferenceRuleSnapshot(matchedRule));
    }

    private Optional<ReferenceRange> findReferenceRange(final ExamResult result, final AppUser user,
            final LocalDate collectedDate) {
        return referenceRangeRepository.findApplicableRangeForTimeline(
                        result.getExamItem().getId(),
                        sexOf(user),
                        ageAt(user, collectedDate),
                        collectedDate)
                .stream()
                .findFirst();
    }

    private Optional<Rule> findMatchedRule(final ReferenceRange referenceRange, final ExamResult result) {
        if (referenceRange == null || result.getValueNumeric() == null) {
            return Optional.empty();
        }

        return referenceRange.getReferenceRangeRules().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> rule.appliesTo(result.getValueNumeric()))
                .min(Comparator.comparing(Rule::getMinValue,
                        Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private static ReferenceRangeSnapshotDTO toReferenceRangeSnapshot(final ReferenceRange referenceRange) {
        return new ReferenceRangeSnapshotDTO(
                referenceRange.getId(),
                referenceRange.getSex(),
                referenceRange.getAgeMinYears(),
                referenceRange.getAgeMaxYears(),
                referenceRange.getVersion(),
                referenceRange.getSource(),
                referenceRange.getValidFrom(),
                referenceRange.getValidUntil());
    }

    private static ReferenceRuleSnapshotDTO toReferenceRuleSnapshot(final Rule rule) {
        return new ReferenceRuleSnapshotDTO(
                rule.getId(),
                rule.getMinValue(),
                rule.getMaxValue(),
                rule.getMinInclusive(),
                rule.getMaxInclusive(),
                rule.getLevel(),
                rule.getDescription(),
                rule.getVersion());
    }

    private static Sex sexOf(final AppUser user) {
        final HealthProfile healthProfile = user.getHealthProfile();
        return healthProfile == null ? null : healthProfile.getSex();
    }

    private static BigDecimal ageAt(final AppUser user, final LocalDate date) {
        if (user.getBirthDate() == null || date == null) {
            return null;
        }
        return BigDecimal.valueOf(ChronoUnit.YEARS.between(user.getBirthDate(), date));
    }

    private static OffsetDateTime startOfDay(final LocalDate date) {
        return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime startOfNextDay(final LocalDate date) {
        return date == null ? null : startOfDay(date.plusDays(NEXT_DAY_OFFSET));
    }

    private void validatePeriod(final LocalDate from, final LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    messageService.getMessage(ApplicationMessage.DOCTOR_TIMELINE_INVALID_PERIOD));
        }
    }
}
