package br.com.fiap.sanguebom.model.dtos;

import br.com.fiap.sanguebom.model.enums.AchivementCode;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class AchievementDTO {

    private Long id;

    private AchivementCode code;

    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private Boolean active;

    private OffsetDateTime createdAt;

}
