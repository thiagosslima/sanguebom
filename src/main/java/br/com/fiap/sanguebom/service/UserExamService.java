package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.mapper.UserExamMapper;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.enums.ApplicationMessage;
import br.com.fiap.sanguebom.model.userexam.ExamDetailDTO;
import br.com.fiap.sanguebom.model.userexam.ExamResultItemDTO;
import br.com.fiap.sanguebom.model.userexam.ExamSummaryDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserExamService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "collectedAt");

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final UserExamMapper userExamMapper;
    private final MessageService messageService;
    private final UserServiceHelper userServiceHelper;

    public UserExamService(final ExamRepository examRepository,
            final ExamResultRepository examResultRepository,
            final UserExamMapper userExamMapper,
            final MessageService messageService,
            final UserServiceHelper userServiceHelper) {
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.userExamMapper = userExamMapper;
        this.messageService = messageService;
        this.userServiceHelper = userServiceHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamSummaryDTO> history(final Long userId, final int page, final int size) {
        userServiceHelper.getUserByIdOrFail(userId);

        return PageResponse.of(examRepository
                .findByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST))
                .map(userExamMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public ExamDetailDTO detail(final Long userId, final Long examId, final Locale locale) {
        final Exam exam = examRepository.findByIdAndUserId(examId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Exame não encontrado para o usuário: %d", userId)));

        final List<ExamResultItemDTO> items =
                userExamMapper.toItems(examResultRepository.findByExamIdWithItem(examId));
        final RiskAssessment risk = exam.getRiskAssessment();

        return new ExamDetailDTO(exam.getId(), exam.getCollectedAt(), exam.getReleasedAt(),
                exam.getStatus(),
                exam.getHealthUnit() == null ? null : exam.getHealthUnit().getName(),
                items,
                risk == null ? null : risk.getScore(),
                risk == null ? null : risk.getLevel(),
                risk == null ? null : risk.getExplanation(),
                messageService.getMessage(ApplicationMessage.EXAM_MEDICAL_DISCLAIMER, locale));
    }

}
