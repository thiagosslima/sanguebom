package br.com.fiap.sanguebom.model.doctor;

import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Valor de um marcador em um exame comparado")
public record ComparedExamValueDTO(

        Long examId,
        BigDecimal valueNumeric,
        String valueText,
        String unit,
        ExamResultFlag flag

) {
}
