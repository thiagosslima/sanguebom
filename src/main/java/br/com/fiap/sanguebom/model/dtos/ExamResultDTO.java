package br.com.fiap.sanguebom.model.dtos;

import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
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
public class ExamResultDTO {

    private Long id;

    @Digits(integer = 14, fraction = 5)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "12.0008")
    private BigDecimal valueNumeric;

    @Size(max = 255)
    private String valueText;

    @Size(max = 50)
    private String unit;

    @Size(max = 30)
    private ExamResultFlag flag;

    private OffsetDateTime createdAt;

    private Long exam;

    private Long examItem;

}
