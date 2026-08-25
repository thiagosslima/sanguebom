package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.RuleDTO;
import br.com.fiap.sanguebom.service.RuleService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/rules", produces = MediaType.APPLICATION_JSON_VALUE)
public class RuleResource {

    private final RuleService ruleService;

    public RuleResource(final RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ResponseEntity<List<RuleDTO>> getAllRules() {
        return ResponseEntity.ok(ruleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleDTO> getRule(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(ruleService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createRule(@RequestBody @Valid final RuleDTO ruleDTO) {
        final Long createdId = ruleService.create(ruleDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateRule(@PathVariable(name = "id") final Long id,
                                           @RequestBody @Valid final RuleDTO ruleDTO) {
        ruleService.update(id, ruleDTO);
        return ResponseEntity.ok(id);
    }
}
