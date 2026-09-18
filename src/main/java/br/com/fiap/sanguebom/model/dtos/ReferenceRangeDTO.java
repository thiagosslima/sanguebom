package br.com.fiap.sanguebom.model.dtos;

import br.com.fiap.sanguebom.model.enums.Sex;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;


@Getter
@Setter
public class ReferenceRangeDTO {

    private Long id;

    private Sex sex;

    @Digits(integer = 5, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "97.08")
    private BigDecimal ageMinYears;

    @Digits(integer = 5, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "57.08")
    private BigDecimal ageMaxYears;

    @Size(max = 30)
    private String version;

    @Size(max = 500)
    private String source;

    private LocalDate validFrom;

    private LocalDate validUntil;

    private OffsetDateTime createdAt;

    private Long examItem;

}
