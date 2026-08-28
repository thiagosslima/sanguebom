package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisResultDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExamAnalysisResultMapper {

    @Mapping(target = "riskAssessmentId", source = "id")
    @Mapping(target = "healthUnitId", source = "exam.healthUnit.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "examId", source = "exam.id")
    ExamAnalysisResultDTO fromRiskAssessmentToAnalysisResult(RiskAssessment entity);
}
