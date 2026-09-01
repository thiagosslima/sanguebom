package br.com.fiap.sanguebom.model.entities;

import br.com.fiap.sanguebom.model.enums.Sex;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class AppUser {

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

    @Column(length = 128)
    private String cpfHash;

    @Column(length = 150)
    private String name;

    @Column
    private LocalDate birthDate;

    @Column
    private String email;

    @Column(length = 20)
    private String status;

    @Column(length = 1)
    @Enumerated(EnumType.STRING)
    private Sex sex;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "user")
    private HealthProfile healthProfile;

    @OneToMany(mappedBy = "user")
    private Set<Exam> userExams = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<RiskAssessment> userRiskAssessments = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Notification> userNotifications = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserAchievement> userUserAchievements = new HashSet<>();
}
