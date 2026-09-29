package br.com.fiap.sanguebom.config.swagger.docs;

import br.com.fiap.sanguebom.config.doc_helper.DefaultBadRequestApiResponse;
import br.com.fiap.sanguebom.config.doc_helper.DefaultNotFoundApiResponse;
import br.com.fiap.sanguebom.model.exam.ExamAnalysisResultDTO;
import br.com.fiap.sanguebom.model.exam.ExamCreateDTO;
import br.com.fiap.sanguebom.model.exam.ExamRecoverDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Exames", description = "Operações relacionadas aos exames")
public interface ExamResourceDocs {

    @Operation(
            summary = "Listar todos os exames",
            description = "Retorna uma lista de todos os exames disponíveis."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de exames retornada com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ExamRecoverDTO.class)))
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<List<ExamRecoverDTO>> getAllExams();

    @Operation(
            summary = "Obter um exame por ID",
            description = "Retorna os detalhes de um exame específico com base no ID fornecido."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Exame retornado com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExamRecoverDTO.class))
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<ExamRecoverDTO> getExam(Long id);

    @Operation(
            summary = "Criar um novo exame",
            description = "Cria um novo exame com base nos dados fornecidos."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Exame criado com sucesso - Retorna os resultados da análise do exame",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExamAnalysisResultDTO.class))
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<ExamAnalysisResultDTO> createExam(ExamCreateDTO examDTO);

    @Operation(
            summary = "Atualizar um exame existente",
            description = "Atualiza os dados de um exame existente com base no ID fornecido."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Exame atualizado com sucesso - Retorna o ID do exame atualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> updateExam(Long id, ExamRecoverDTO examRecoverDTO);
}
