package br.com.fiap.sanguebom.model.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da varredura diaria da meta de exames (CF-347)")
public record ExamGoalScanResultDTO(

        @Schema(description = "Cidadaos ativos avaliados")
        int scanned,

        @Schema(description = "Avisos de vencimento proximo criados nesta execucao")
        int dueSoon,

        @Schema(description = "Avisos de meta vencida criados nesta execucao")
        int overdue,

        @Schema(description = "Cidadaos ignorados por falta de dados, por exemplo sem perfil de saude")
        int skipped

) {
}
