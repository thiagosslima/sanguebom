package br.com.fiap.sanguebom.model.entities;

import br.com.fiap.sanguebom.model.enums.Sex;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "reference_range")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ReferenceRange {

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

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Sex sex;

    @Column(precision = 5, scale = 2)
    private BigDecimal ageMinYears;

    @Column(precision = 5, scale = 2)
    private BigDecimal ageMaxYears;

    @Column(length = 30)
    private String version;

    @Column(length = 500)
    private String source;

    @Column
    private LocalDate validFrom;

    @Column
    private LocalDate validUntil;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_item_id")
    private ExamItem examItem;

    @OneToMany(mappedBy = "referenceRange")
    private Set<Rule> referenceRangeRules = new HashSet<>();
}