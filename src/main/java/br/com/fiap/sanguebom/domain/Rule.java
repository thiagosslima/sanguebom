package br.com.fiap.sanguebom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "Rules")
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
    private String level;

    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    @Column(length = 500)
    private String description;

    @Column(length = 30)
    private String version;

    @Column
    private Boolean active;

    @Column
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_range_id")
    private ReferenceRange referenceRange;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}
