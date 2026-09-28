package br.com.fiap.sanguebom.model.entities;

import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;


@Entity
@Table(name = "notification")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Notification {

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

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String message;

    @Column
    private OffsetDateTime scheduledAt;

    @Column
    private OffsetDateTime sentAt;

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(length = 120)
    private String referenceKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;
}
