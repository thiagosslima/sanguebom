package br.com.fiap.sanguebom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserAchievementId {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "achievement_id")
    private Long achievementId;
}
