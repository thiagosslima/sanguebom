package br.com.fiap.sanguebom.model.dtos;

import br.com.fiap.sanguebom.model.enums.Sex;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Getter
@Setter
public class HealthProfileDTO {

    private Long id;

    @Size(max = 20)
    private Sex sex;

    @Digits(integer = 5, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "75.08")
    private BigDecimal heightCm;

    @Digits(integer = 6, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "84.08")
    private BigDecimal weightKg;

    private String riskFactors;

    @NotNull
    @Size(max = 20)
    private String examPeriodicity;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Long user;

}
