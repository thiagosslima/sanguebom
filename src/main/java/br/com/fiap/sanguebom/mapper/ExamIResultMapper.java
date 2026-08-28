package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExamIResultMapper {

    @Mapping(target = "valueNumeric", source = "measuredValue")
    ExamResult toEntity(ExamResultDTO examItemCreateDTO);
}
