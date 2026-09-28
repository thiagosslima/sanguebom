package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.dtos.RiskAssessmentDTO;
import br.com.fiap.sanguebom.model.riskAssessment.RiskTimelinePointDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@Validated
@RequestMapping(value = "/api/riskAssessments", produces = MediaType.APPLICATION_JSON_VALUE)
public class RiskAssessmentResource {

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentResource(final RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @GetMapping
    public ResponseEntity<List<RiskAssessmentDTO>> getAllRiskAssessments() {
        return ResponseEntity.ok(riskAssessmentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskAssessmentDTO> getRiskAssessment(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(riskAssessmentService.get(id));
    }

    @GetMapping("/{userId}/timeline")
    @Operation(summary = "Evolução temporal de um risco do paciente",
            description = "Retorna uma página da série histórica de um risco do paciente, "
                    + "com a pontuação e a classificação em cada data.")
    public ResponseEntity<PageResponse<RiskTimelinePointDTO>> getTimeline(
            @PathVariable(name = "userId") final Long userId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) final int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) final int size) {
        return ResponseEntity.ok(riskAssessmentService.findByUserId(userId, from, to, page, size));
    }


    @PutMapping("/{id}")
    public ResponseEntity<Long> updateRiskAssessment(@PathVariable(name = "id") final Long id,
                                                     @RequestBody @Valid final RiskAssessmentDTO riskAssessmentDTO) {
        riskAssessmentService.update(id, riskAssessmentDTO);
        return ResponseEntity.ok(id);
    }
}
