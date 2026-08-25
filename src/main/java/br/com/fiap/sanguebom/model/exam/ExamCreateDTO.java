package br.com.fiap.sanguebom.model.exam;

import br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public record ExamCreateDTO(

        @NotBlank
        OffsetDateTime collectedAt,

        @Size(max = 100)
        //id externo do exame no laboratório
        String externalReference,

        @NotNull
        Long userId,

        @NotNull
        Long healthUnitId,

        @Valid
        @NotEmpty
        List<ExamResultDTO> analyzedItems
) {
}

