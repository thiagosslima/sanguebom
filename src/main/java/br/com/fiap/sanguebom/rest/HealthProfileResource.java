package br.com.fiap.sanguebom.rest;

import br.com.fiap.sanguebom.model.HealthProfileDTO;
import br.com.fiap.sanguebom.service.HealthProfileService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/healthProfiles", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthProfileResource {

    private final HealthProfileService healthProfileService;

    public HealthProfileResource(final HealthProfileService healthProfileService) {
        this.healthProfileService = healthProfileService;
    }

    @GetMapping
    public ResponseEntity<List<HealthProfileDTO>> getAllHealthProfiles() {
        return ResponseEntity.ok(healthProfileService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthProfileDTO> getHealthProfile(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(healthProfileService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createHealthProfile(
            @RequestBody @Valid final HealthProfileDTO healthProfileDTO) {
        final Long createdId = healthProfileService.create(healthProfileDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateHealthProfile(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final HealthProfileDTO healthProfileDTO) {
        healthProfileService.update(id, healthProfileDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteHealthProfile(@PathVariable(name = "id") final Long id) {
        healthProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
