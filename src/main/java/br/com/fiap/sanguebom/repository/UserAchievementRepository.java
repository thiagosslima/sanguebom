package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    boolean existsByUserIdAndAchievementIdAndIdNot(Long userId, Long achievementId, Long id);
}
