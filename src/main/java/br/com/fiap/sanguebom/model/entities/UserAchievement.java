package br.com.fiap.sanguebom.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;


@Entity
@Table(
        name = "user_achievement",
        uniqueConstraints = @UniqueConstraint(
                name = "user_achievement_user_achievement_uidx",
                columnNames = {"user_id", "achievement_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class UserAchievement {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime earnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;
}
