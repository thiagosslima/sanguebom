package br.com.fiap.sanguebom.model.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class UserAchievementDTO {

    private Long id;

    private OffsetDateTime earnedAt;

    @NotNull
    private Long user;

    @NotNull
    private Long achievement;

}
