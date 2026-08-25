package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.ReferenceRangeDTO;
import br.com.fiap.sanguebom.service.ReferenceRangeService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/referenceRanges", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReferenceRangeResource {

    private final ReferenceRangeService referenceRangeService;

    public ReferenceRangeResource(final ReferenceRangeService referenceRangeService) {
        this.referenceRangeService = referenceRangeService;
    }

    @GetMapping
    public ResponseEntity<List<ReferenceRangeDTO>> getAllReferenceRanges() {
        return ResponseEntity.ok(referenceRangeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferenceRangeDTO> getReferenceRange(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(referenceRangeService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createReferenceRange(
            @RequestBody @Valid final ReferenceRangeDTO referenceRangeDTO) {
        final Long createdId = referenceRangeService.create(referenceRangeDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateReferenceRange(@PathVariable(name = "id") final Long id,
                                                     @RequestBody @Valid final ReferenceRangeDTO referenceRangeDTO) {
        referenceRangeService.update(id, referenceRangeDTO);
        return ResponseEntity.ok(id);
    }
}
