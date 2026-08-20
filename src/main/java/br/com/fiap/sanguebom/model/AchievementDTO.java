package br.com.fiap.sanguebom.model;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class AchievementDTO {

    private Long id;

    @Size(max = 50)
    private String code;

    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private Boolean active;

    private OffsetDateTime createdAt;

}
