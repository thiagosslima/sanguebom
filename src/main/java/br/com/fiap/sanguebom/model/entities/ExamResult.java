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
@Table(name = "exam_result")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ExamResult {

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

    @Column(precision = 14, scale = 5)
    private BigDecimal valueNumeric;

    @Column
    private String valueText;

    @Column(length = 50)
    private String unit;

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    private ExamResultFlag flag;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_item_id")
    private ExamItem examItem;

}
