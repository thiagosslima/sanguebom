package br.com.fiap.sanguebom.mapper;


import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
import br.com.fiap.sanguebom.model.exam.ExamRecoverDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamMapper {

    Exam toEntity(ExamCreateDTO examCreateDTO);

    Exam toEntity(ExamRecoverDTO examCreateDTO);

    /**
     * Copia os campos editaveis do DTO sobre um exame ja carregado. As associacoes e o id
     * ficam de fora: quem resolve user e healthUnit a partir dos ids do DTO e o service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "healthUnit", ignore = true)
    @Mapping(target = "examResults", ignore = true)
    @Mapping(target = "riskAssessment", ignore = true)
    void updateEntity(@MappingTarget Exam exam, ExamRecoverDTO examRecoverDTO);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "healthUnitId", source = "healthUnit.id")
    ExamRecoverDTO toDTO(Exam exam);
}
