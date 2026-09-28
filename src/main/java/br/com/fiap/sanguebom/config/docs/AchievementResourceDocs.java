package br.com.fiap.sanguebom.config.docs;

import br.com.fiap.sanguebom.config.doc_helper.DefaultBadRequestApiResponse;
import br.com.fiap.sanguebom.config.doc_helper.DefaultNotFoundApiResponse;
import br.com.fiap.sanguebom.model.dtos.AchievementDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Conquistas", description = "Operações relacionadas às conquistas")
public interface AchievementResourceDocs {

    @Operation(
            summary = "Listar todas as conquistas",
            description = "Retorna uma lista de todas as conquistas disponíveis.")
    @SwaggerDocumentation("achievements/listar-conquistas.md")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de conquistas retornadas com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = AchievementDTO.class)))
    )
    ResponseEntity<List<AchievementDTO>> getAllAchievements();

    @Operation(
            summary = "Buscar conquista por ID",
            description = "Retorna os detalhes de uma conquista específica com base no ID fornecido."
    )
    @SwaggerDocumentation("achievements/buscar-conquista.md")
    @DefaultNotFoundApiResponse
    ResponseEntity<AchievementDTO> getAchievement(@PathVariable(name = "id") final Long id);

    @Operation(
            summary = "Criar nova conquista",
            description = "Cria uma nova conquista com base nos dados fornecidos."
    )
    @SwaggerDocumentation("achievements/criar-conquista.md")
    @ApiResponse(
            responseCode = "201",
            description = "Conquista criada com sucesso - Retorna o ID da nova conquista",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> createAchievement(
            @RequestBody @Valid final AchievementDTO achievementDTO);

    @Operation(
            summary = "Atualizar conquista",
            description = "Atualiza os dados de uma conquista existente com base no ID fornecido."
    )
    @SwaggerDocumentation("achievements/atualizar-conquista.md")
    @ApiResponse(
            responseCode = "200",
            description = "Conquista atualizada com sucesso - Retorna o ID da conquista atualizada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> updateAchievement(@PathVariable(name = "id") final Long id,
                                           @RequestBody @Valid final AchievementDTO achievementDTO);
}
