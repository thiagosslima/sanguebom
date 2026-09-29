package br.com.fiap.sanguebom.model.userexam;

import br.com.fiap.sanguebom.model.enums.ExamStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Resumo de um exame na listagem do historico")
public record ExamSummaryDTO(

        Long id,
        OffsetDateTime collectedAt,
        OffsetDateTime releasedAt,
        ExamStatus status,

        @Schema(description = "Nome da unidade de saude que realizou o exame")
        String healthUnitName

) {
}
