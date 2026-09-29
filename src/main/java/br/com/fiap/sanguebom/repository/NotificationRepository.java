package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationStatus;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndTypeAndReferenceKey(Long userId, NotificationType type, String referenceKey);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtAsc(Long userId, NotificationStatus status);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
