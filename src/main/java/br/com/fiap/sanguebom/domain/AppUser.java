package br.com.fiap.sanguebom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "AppUsers")
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

    @Column
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    private Set<HealthProfile> userHealthProfiles = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Exam> userExams = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<BloodPressure> userBloodPressures = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<RiskAssessment> userRiskAssessments = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Notification> userNotifications = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserAchievement> userUserAchievements = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}
