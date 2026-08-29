package br.com.fiap.sanguebom.model.userexam;

import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Detalhe de um exame com os itens medidos e o pre-diagnostico")
public record ExamDetailDTO(

        Long id,
        OffsetDateTime collectedAt,
        OffsetDateTime releasedAt,
        ExamStatus status,
        String healthUnitName,
        List<ExamResultItemDTO> items,

        @Schema(description = "Score de risco; nulo enquanto o motor de regras nao avaliou o exame")
        BigDecimal riskScore,

        @Schema(description = "Nivel de risco; nulo enquanto o motor de regras nao avaliou o exame")
        RiskAssessmentLevel riskLevel,

        @Schema(description = "Explicacao do pre-diagnostico em linguagem simples")
        String explanation,

        @Schema(description = "Aviso medico obrigatorio")
        String disclaimer

) {
}
