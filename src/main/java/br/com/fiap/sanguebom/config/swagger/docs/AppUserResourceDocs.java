package br.com.fiap.sanguebom.config.swagger.docs;

import br.com.fiap.sanguebom.config.doc_helper.DefaultBadRequestApiResponse;
import br.com.fiap.sanguebom.config.doc_helper.DefaultNotFoundApiResponse;
import br.com.fiap.sanguebom.model.dtos.AppUserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Usuários do Aplicativo", description = "Operações relacionadas aos usuários do aplicativo")
public interface AppUserResourceDocs {

    @Operation(
            summary = "Listar todos os usuários do aplicativo",
            description = "Retorna uma lista de todos os usuários do aplicativo cadastrados no sistema."
    )
    @SwaggerDocumentation("app-users/listar-cidadaos.md")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de usuários retornada com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = AppUserDTO.class))
            )
    )
    ResponseEntity<List<AppUserDTO>> getAllAppUsers();

    @Operation(
            summary = "Buscar usuário por ID",
            description = "Retorna os detalhes de um usuário específico com base no ID fornecido."
    )
    @SwaggerDocumentation("app-users/buscar-cidadao.md")
    @ApiResponse(
            responseCode = "200",
            description = "Usuário encontrado com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppUserDTO.class)
            )
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<AppUserDTO> getAppUser(Long id);

    @Operation(
            summary = "Criar novo usuário do aplicativo",
            description = "Cria um novo usuário do aplicativo com base nos dados fornecidos."
    )
    @SwaggerDocumentation("app-users/criar-cidadao.md")
    @ApiResponse(
            responseCode = "201",
            description = "Usuário criado com sucesso - Retorna o ID do novo usuário",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> createAppUser(AppUserDTO appUserDTO);

    @Operation(
            summary = "Atualizar usuário do aplicativo",
            description = "Atualiza os dados de um usuário existente com base no ID fornecido."
    )
    @SwaggerDocumentation("app-users/atualizar-cidadao.md")
    @ApiResponse(
            responseCode = "200",
            description = "Usuário atualizado com sucesso - Retorna o ID do usuário atualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "integer", format = "int64")
            )
    )
    @DefaultBadRequestApiResponse
    ResponseEntity<Long> updateAppUser(Long id, AppUserDTO appUserDTO);
}
