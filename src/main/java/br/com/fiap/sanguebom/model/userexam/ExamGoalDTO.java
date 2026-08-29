package br.com.fiap.sanguebom.model.userexam;

import br.com.fiap.sanguebom.model.enums.ExamGoalStatus;
import br.com.fiap.sanguebom.model.enums.ExamPeriodicity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Meta de exames: quando foi o ultimo exame e quando vence o proximo")
public record ExamGoalDTO(

        @Schema(description = "Data de coleta do ultimo exame; nulo quando nao ha historico")
        LocalDate lastExamDate,

        @Schema(description = "Data limite para o proximo exame; nulo quando nao ha historico")
        LocalDate dueDate,

        @Schema(description = "Dias ate o vencimento; negativo quando vencido, nulo quando nao ha historico")
        Long daysRemaining,

        @Schema(description = "Periodicidade configurada no perfil de saude")
        ExamPeriodicity periodicity,

        ExamGoalStatus status

) {
}
