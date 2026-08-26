package br.com.fiap.sanguebom.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "health_unit")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class HealthUnit {

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

    @Column(length = 200)
    private String name;

    @Column(length = 20)
    private String cnes;

    @Column(length = 30)
    private String type;

    @Column(length = 20)
    private String status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "healthUnit")
    private Set<Exam> healthUnitExams = new HashSet<>();
}
