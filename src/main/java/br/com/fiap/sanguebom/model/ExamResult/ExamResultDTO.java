package br.com.fiap.sanguebom.model.ExamResult;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExamResultDTO(
        @NotNull
        Long examItemId,

        @NotNull
        @DecimalMin(value = "0", inclusive = true, message = "Valor medido deve ser maior ou igual a 0")
        BigDecimal measuredValue
) {
}
