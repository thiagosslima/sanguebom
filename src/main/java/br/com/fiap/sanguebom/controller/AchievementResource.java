package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.dtos.AchievementDTO;
import br.com.fiap.sanguebom.service.AchievementService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/achievements", produces = MediaType.APPLICATION_JSON_VALUE)
public class AchievementResource {

    private final AchievementService achievementService;

    public AchievementResource(final AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public ResponseEntity<List<AchievementDTO>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AchievementDTO> getAchievement(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(achievementService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createAchievement(
            @RequestBody @Valid final AchievementDTO achievementDTO) {
        final Long createdId = achievementService.create(achievementDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateAchievement(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final AchievementDTO achievementDTO) {
        achievementService.update(id, achievementDTO);
        return ResponseEntity.ok(id);
    }
}
