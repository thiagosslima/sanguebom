package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.dtos.UserAchievementDTO;
import br.com.fiap.sanguebom.service.UserAchievementService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/userAchievements", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserAchievementResource {

    private final UserAchievementService userAchievementService;

    public UserAchievementResource(final UserAchievementService userAchievementService) {
        this.userAchievementService = userAchievementService;
    }

    @GetMapping
    public ResponseEntity<List<UserAchievementDTO>> getAllUserAchievements() {
        return ResponseEntity.ok(userAchievementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAchievementDTO> getUserAchievement(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(userAchievementService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createUserAchievement(
            @RequestBody @Valid final UserAchievementDTO userAchievementDTO) {
        final Long createdId = userAchievementService.create(userAchievementDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateUserAchievement(@PathVariable(name = "id") final Long id,
                                                      @RequestBody @Valid final UserAchievementDTO userAchievementDTO) {
        userAchievementService.update(id, userAchievementDTO);
        return ResponseEntity.ok(id);
    }
}
