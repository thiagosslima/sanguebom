package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.domain.ExamResult;
import br.com.fiap.sanguebom.model.ExamResult.ExamResultDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExamIResultMapper {

    ExamResult toEntity(ExamResultDTO examItemCreateDTO);
}
