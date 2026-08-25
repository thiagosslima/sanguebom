package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;


public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Notification findFirstByUserIdOrderByLastUpdatedDesc(Long userId);
}
