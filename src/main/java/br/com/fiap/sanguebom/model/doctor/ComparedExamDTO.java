package br.com.fiap.sanguebom.model.doctor;

import br.com.fiap.sanguebom.model.enums.ExamStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Exame incluido na comparacao medica")
public record ComparedExamDTO(

        Long examId,
        OffsetDateTime collectedAt,
        OffsetDateTime releasedAt,
        ExamStatus status

) {
}
