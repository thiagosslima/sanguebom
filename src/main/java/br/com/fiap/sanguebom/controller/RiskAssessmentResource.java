package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.dtos.RiskAssessmentDTO;
import br.com.fiap.sanguebom.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
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

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createRiskAssessment(
            @RequestBody @Valid final RiskAssessmentDTO riskAssessmentDTO) {
        final Long createdId = riskAssessmentService.create(riskAssessmentDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateRiskAssessment(@PathVariable(name = "id") final Long id,
                                                     @RequestBody @Valid final RiskAssessmentDTO riskAssessmentDTO) {
        riskAssessmentService.update(id, riskAssessmentDTO);
        return ResponseEntity.ok(id);
    }
}
