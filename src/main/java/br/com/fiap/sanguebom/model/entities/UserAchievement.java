package br.com.fiap.sanguebom.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;


@Entity
@Table(name = "user_achievement")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class UserAchievement {

    @EmbeddedId
    private UserAchievementId id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime earnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            insertable = false,
            updatable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id",
            insertable = false,
            updatable = false)
    private Achievement achievement;
}
