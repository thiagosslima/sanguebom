package br.com.fiap.sanguebom.model.entities;

import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "rule")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Rule {

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

    @Column(precision = 12, scale = 4)
    private BigDecimal minValue;

    @Column(precision = 12, scale = 4)
    private BigDecimal maxValue;

    @Column
    private Boolean minInclusive;

    @Column
    private Boolean maxInclusive;

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    private ExamResultFlag level;

    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    @Column(length = 500)
    private String description;

    @Column(length = 30)
    private String version;

    @Column
    private Boolean active;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_range_id")
    private ReferenceRange referenceRange;

    public boolean appliesTo(BigDecimal value) {

        boolean respectsMin = true;
        boolean respectsMax = true;

        if (minValue != null) {
            respectsMin = Boolean.TRUE.equals(minInclusive)
                    ? value.compareTo(minValue) >= 0
                    : value.compareTo(minValue) > 0;
        }

        if (maxValue != null) {
            respectsMax = Boolean.TRUE.equals(maxInclusive)
                    ? value.compareTo(maxValue) <= 0
                    : value.compareTo(maxValue) < 0;
        }

        return respectsMin && respectsMax;
    }

}
