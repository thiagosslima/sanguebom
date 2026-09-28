package br.com.fiap.sanguebom.config.swagger.docs;

import br.com.fiap.sanguebom.config.doc_helper.DefaultBadRequestApiResponse;
import br.com.fiap.sanguebom.config.doc_helper.DefaultNotFoundApiResponse;
import br.com.fiap.sanguebom.model.dtos.ExamItemDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Itens de Exame", description = "Operações relacionadas aos itens de exame")
public interface ExamItemResourceDocs {

    @Operation(
            summary = "Listar todos os itens de exame",
            description = "Retorna uma lista de todos os itens de exame disponíveis."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de itens de exame retornada com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ExamItemDTO.class)))
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<List<ExamItemDTO>> getAllExamItems();

    @Operation(
            summary = "Buscar item de exame por ID",
            description = "Retorna os detalhes de um item de exame específico com base no ID fornecido."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Item de exame retornado com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExamItemDTO.class))
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<ExamItemDTO> getExamItem(Long id);

    @Operation(
            summary = "Criar novo item de exame",
            description = "Cria um novo item de exame com base nos dados fornecidos."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Item de exame criado com sucesso - Retorna o ID do novo item de exame",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> createExamItem(ExamItemDTO examItemDTO);

    @Operation(
            summary = "Atualizar item de exame",
            description = "Atualiza os dados de um item de exame existente com base no ID fornecido."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Item de exame atualizado com sucesso - Retorna o ID do item de exame atualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<Long> updateExamItem(Long id, ExamItemDTO examItemDTO);
}
