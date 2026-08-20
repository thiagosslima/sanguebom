package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
}
