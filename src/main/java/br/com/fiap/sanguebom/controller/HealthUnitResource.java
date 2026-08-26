package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.dtos.HealthUnitDTO;
import br.com.fiap.sanguebom.service.HealthUnitService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/healthUnits", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthUnitResource {

    private final HealthUnitService healthUnitService;

    public HealthUnitResource(final HealthUnitService healthUnitService) {
        this.healthUnitService = healthUnitService;
    }

    @GetMapping
    public ResponseEntity<List<HealthUnitDTO>> getAllHealthUnits() {
        return ResponseEntity.ok(healthUnitService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthUnitDTO> getHealthUnit(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(healthUnitService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createHealthUnit(
            @RequestBody @Valid final HealthUnitDTO healthUnitDTO) {
        final Long createdId = healthUnitService.create(healthUnitDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateHealthUnit(@PathVariable(name = "id") final Long id,
                                                 @RequestBody @Valid final HealthUnitDTO healthUnitDTO) {
        healthUnitService.update(id, healthUnitDTO);
        return ResponseEntity.ok(id);
    }
}
