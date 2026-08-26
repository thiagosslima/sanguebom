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
@Table(name = "exam_item")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ExamItem {

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

    @Column(length = 50)
    private String code;

    @Column(length = 150)
    private String name;

    @Column(length = 50)
    private String unit;

    @Column(length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    @Column
    private Boolean active;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "examItem")
    private Set<ExamResult> examItemExamResults = new HashSet<>();

    @OneToMany(mappedBy = "examItem")
    private Set<ReferenceRange> examItemReferenceRanges = new HashSet<>();
}
