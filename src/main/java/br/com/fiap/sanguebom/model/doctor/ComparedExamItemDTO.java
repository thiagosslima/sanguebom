package br.com.fiap.sanguebom.model.doctor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Marcador comparado entre exames")
public record ComparedExamItemDTO(

        String itemCode,
        String itemName,
        String unit,
        List<ComparedExamValueDTO> values,
        List<ExamItemVariationDTO> variations

) {
}
