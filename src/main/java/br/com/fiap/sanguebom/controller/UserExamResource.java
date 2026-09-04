package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.userexam.ExamDetailDTO;
import br.com.fiap.sanguebom.model.userexam.ExamGoalDTO;
import br.com.fiap.sanguebom.model.userexam.ExamSummaryDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.ExamGoalService;
import br.com.fiap.sanguebom.service.UserExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;


@RestController
@Validated
@Tag(name = "Exames do usuário", description = "Meta de exames, histórico e detalhe do exame do cidadão")
@RequestMapping(value = "/api/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserExamResource {

    private final ExamGoalService examGoalService;
    private final UserExamService userExamService;

    public UserExamResource(final ExamGoalService examGoalService,
            final UserExamService userExamService) {
        this.examGoalService = examGoalService;
        this.userExamService = userExamService;
    }

    @GetMapping("/exam-goal")
    @Operation(summary = "Meta de exames",
            description = "Data do último exame, data de vencimento e situação: UP_TO_DATE, DUE_SOON, "
                    + "OVERDUE ou NO_HISTORY quando o cidadão ainda não realizou nenhum exame.")
    public ResponseEntity<ExamGoalDTO> getExamGoal(@PathVariable(name = "userId") final Long userId) {
        return ResponseEntity.ok(examGoalService.goalOf(userId));
    }

    @GetMapping("/exams")
    @Operation(summary = "Histórico de exames",
            description = "Exames do próprio cidadão, paginados e ordenados da data de coleta mais "
                    + "recente para a mais antiga.")
    public ResponseEntity<PageResponse<ExamSummaryDTO>> getExams(
            @PathVariable(name = "userId") final Long userId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) final int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) final int size) {
        return ResponseEntity.ok(userExamService.history(userId, page, size));
    }

    @GetMapping("/exams/{examId}")
    @Operation(summary = "Detalhe do exame",
            description = "Cada item medido com valor, unidade e classificação, além do score, do nível "
                    + "de risco, da explicação em linguagem simples e do aviso médico obrigatório. Um "
                    + "exame que não pertença ao cidadão informado retorna 404.")
    public ResponseEntity<ExamDetailDTO> getExam(@PathVariable(name = "userId") final Long userId,
            @PathVariable(name = "examId") final Long examId,
            final Locale locale) {
        return ResponseEntity.ok(userExamService.detail(userId, examId, locale));
    }

}
