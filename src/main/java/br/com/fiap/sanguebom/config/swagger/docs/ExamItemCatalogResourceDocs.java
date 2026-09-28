package br.com.fiap.sanguebom.config.swagger.docs;

import br.com.fiap.sanguebom.config.doc_helper.DefaultBadRequestApiResponse;
import br.com.fiap.sanguebom.config.doc_helper.DefaultNotFoundApiResponse;
import br.com.fiap.sanguebom.config.swagger.schema.ExamItemCatalogResponseSchema;
import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Catálogo público de exames",
        description = "Itens de exame e faixas de referência vigentes usadas pelo sistema")
public interface ExamItemCatalogResourceDocs {

    @Operation(
            summary = "Lista itens de exame ativos",
            description = "Consulta pública paginada dos itens de exame disponíveis no catálogo do sistema.")
    @SwaggerDocumentation("exam-item-catalog/listar-itens.md")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de itens de exame retornada com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExamItemCatalogResponseSchema.class)
            )
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<PageResponse<ExamItemCatalogDTO>> getExamItems(String category, int page, int size);

    @Operation(
            summary = "Lista faixas de referência vigentes por item",
            description = "Consulta pública das faixas de referência que o sistema usa hoje, "
                    + "incluindo a fonte e as regras numéricas associadas.")
    @SwaggerDocumentation("exam-item-catalog/listar-faixas-referencia.md")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de faixas de referência retornada com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ReferenceRangeCatalogDTO.class))
            )
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<List<ReferenceRangeCatalogDTO>> getReferenceRanges(String code);
}
