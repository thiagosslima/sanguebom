package br.com.fiap.sanguebom.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "exam")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Exam {

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

    @Column
    private OffsetDateTime collectedAt;

    @Column
    private OffsetDateTime releasedAt;

    @Column(length = 30)
    private String status;

    @Column(length = 100)
    private String externalReference;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_unit_id")
    private HealthUnit healthUnit;

    @OneToMany(mappedBy = "exam")
    private Set<ExamResult> examExamResults = new HashSet<>();

    @OneToMany(mappedBy = "exam")
    private Set<RiskAssessment> examRiskAssessments = new HashSet<>();
}
