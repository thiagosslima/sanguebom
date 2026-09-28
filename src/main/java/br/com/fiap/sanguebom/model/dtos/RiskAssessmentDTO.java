package br.com.fiap.sanguebom.model.dtos;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
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
public class RiskAssessmentDTO {

    private Long id;

    @Digits(integer = 5, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "34.08")
    private BigDecimal score;

    private RiskAssessmentLevel level;

    @Size(max = 30)
    private String rulesVersion;

    private String explanation;

    private OffsetDateTime createdAt;

    private Long user;

    private Long exam;

}
