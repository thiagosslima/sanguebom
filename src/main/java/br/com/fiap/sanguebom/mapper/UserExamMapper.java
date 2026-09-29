package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.userexam.ExamResultItemDTO;
import br.com.fiap.sanguebom.model.userexam.ExamSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserExamMapper {

    @Mapping(target = "healthUnitName", source = "healthUnit.name")
    ExamSummaryDTO toSummary(Exam exam);

    @Mapping(target = "itemCode", source = "examItem.code")
    @Mapping(target = "itemName", source = "examItem.name")
    @Mapping(target = "unit",
            expression = "java(result.getUnit() != null ? result.getUnit() : result.getExamItem().getUnit())")
    ExamResultItemDTO toItem(ExamResult result);

    List<ExamResultItemDTO> toItems(List<ExamResult> results);
}
