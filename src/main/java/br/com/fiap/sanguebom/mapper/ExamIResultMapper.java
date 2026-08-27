package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExamIResultMapper {

    ExamResult toEntity(ExamResultDTO examItemCreateDTO);
}
