package br.com.fiap.sanguebom.config.swagger.docs;

import br.com.fiap.sanguebom.config.doc_helper.DefaultBadRequestApiResponse;
import br.com.fiap.sanguebom.model.dtos.ExamResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Resultados de Exames", description = "Operações relacionadas aos resultados de exames")
public interface ExamResultResourceDocs {

    @Operation(
            summary = "Listar todos os resultados de exames",
            description = "Retorna uma lista de todos os resultados de exames disponíveis."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de resultados de exames retornados com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ExamResultDTO.class)))
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<List<ExamResultDTO>> getAllExamResults();

    @Operation(
            summary = "Buscar resultado de exame por ID",
            description = "Retorna os detalhes de um resultado de exame específico com base no ID fornecido."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Resultado de exame retornado com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExamResultDTO.class))
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<ExamResultDTO> getExamResult(Long id);

    @Operation(
            summary = "Criar novo resultado de exame",
            description = "Cria um novo resultado de exame com base nos dados fornecidos."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Resultado de exame criado com sucesso - Retorna o ID do novo resultado de exame",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> createExamResult(ExamResultDTO examResultDTO);

    @Operation(
            summary = "Atualizar resultado de exame",
            description = "Atualiza os dados de um resultado de exame existente com base no ID fornecido."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Resultado de exame atualizado com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> updateExamResult(Long id, ExamResultDTO examResultDTO);
}
