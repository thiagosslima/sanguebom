package br.com.fiap.sanguebom.controller.doc;

import br.com.fiap.sanguebom.model.dtos.AchievementDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Conquistas", description = "Operações relacionadas às conquistas")
public interface AchievementResourceDoc {

    @SwaggerDocumentation("achievements/listar-conquistas.md")
    ResponseEntity<List<AchievementDTO>> getAllAchievements();

    @SwaggerDocumentation("achievements/buscar-conquistas.md")
    ResponseEntity<AchievementDTO> getAchievement(@PathVariable(name = "id") final Long id);

    @SwaggerDocumentation("achievements/criar-conquistas.md")
    ResponseEntity<Long> createAchievement(
            @RequestBody @Valid final AchievementDTO achievementDTO);

    @SwaggerDocumentation("achievements/atualizar-conquistas.md")
    ResponseEntity<Long> updateAchievement(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final AchievementDTO achievementDTO);
}
