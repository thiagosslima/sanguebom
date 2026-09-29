package br.com.fiap.sanguebom.model.userexam;

import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Item medido dentro de um exame, com o valor e a classificacao do pre-diagnostico")
public record ExamResultItemDTO(

        String itemCode,
        String itemName,
        BigDecimal valueNumeric,
        String valueText,
        String unit,

        @Schema(description = "Classificacao do item pelo motor de regras")
        ExamResultFlag flag

) {
}
