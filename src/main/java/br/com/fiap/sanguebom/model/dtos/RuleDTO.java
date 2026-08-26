package br.com.fiap.sanguebom.model.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Getter
@Setter
public class RuleDTO {

    private Long id;

    @Digits(integer = 12, fraction = 4)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "49.0008")
    private BigDecimal minValue;

    @Digits(integer = 12, fraction = 4)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "75.0008")
    private BigDecimal maxValue;

    private Boolean minInclusive;

    private Boolean maxInclusive;

    @Size(max = 30)
    private String level;

    @Digits(integer = 4, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "34.08")
    private BigDecimal score;

    @Size(max = 500)
    private String description;

    @Size(max = 30)
    private String version;

    private Boolean active;

    private OffsetDateTime createdAt;

    private Long referenceRange;

}
