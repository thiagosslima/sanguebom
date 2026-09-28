package br.com.fiap.sanguebom.model.doctor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Comparacao lado a lado de dois ou mais exames")
public record ExamComparisonDTO(

        Long userId,
        List<ComparedExamDTO> exams,
        List<ComparedExamItemDTO> items

) {
}
