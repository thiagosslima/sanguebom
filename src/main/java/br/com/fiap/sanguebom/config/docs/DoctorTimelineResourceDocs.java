package br.com.fiap.sanguebom.config.docs;

import br.com.fiap.sanguebom.config.doc_helper.DefaultBadRequestApiResponse;
import br.com.fiap.sanguebom.config.doc_helper.DefaultNotFoundApiResponse;
import br.com.fiap.sanguebom.model.doctor.ExamComparisonDTO;
import br.com.fiap.sanguebom.model.doctor.MarkerTimelinePointDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Médico", description = "Consultas longitudinais de exames para apoio ao atendimento")
public interface DoctorTimelineResourceDocs {

    @Operation(
            summary = "Evolução temporal de um marcador",
            description = "Retorna uma página da série histórica de um marcador do paciente, "
                    + "com valor, unidade, classificação e faixa de referência vigente em cada data."
    )
    @SwaggerDocumentation("doctor/timeline.md")
    @ApiResponse(
            responseCode = "200",
            description = "Página da evolução temporal do marcador retornada com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = MarkerTimelinePointDTO.class))
            )
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<PageResponse<MarkerTimelinePointDTO>> getTimeline(
            Long userId, String itemCode, LocalDate from, LocalDate to, int page, int size);

    @Operation(
            summary = "Comparação lado a lado de exames",
            description = "Retorna os itens medidos em dois ou mais exames do paciente, "
                    + "com valores lado a lado e variações absoluta e percentual entre exames consecutivos."
    )
    @SwaggerDocumentation("doctor/compare-exams.md")
    @ApiResponse(
            responseCode = "200",
            description = "Página da comparação de exames retornada com sucesso.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ExamComparisonDTO.class))
            )
    )
    @DefaultBadRequestApiResponse
    @DefaultNotFoundApiResponse
    ResponseEntity<ExamComparisonDTO> compareExams(Long userId, List<Long> examIds);
}
