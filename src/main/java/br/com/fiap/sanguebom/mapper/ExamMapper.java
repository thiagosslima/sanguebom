package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.domain.Exam;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExamMapper {

    Exam toEntity(ExamCreateDTO examCreateDTO);
}
