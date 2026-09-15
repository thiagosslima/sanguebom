package br.com.fiap.sanguebom.model.doctor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Variacao de um marcador entre dois exames consecutivos")
public record ExamItemVariationDTO(

        Long fromExamId,
        Long toExamId,
        BigDecimal absoluteVariation,
        BigDecimal percentVariation

) {
}
