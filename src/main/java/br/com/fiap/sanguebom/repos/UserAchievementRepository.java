package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    @Query("select ua from UserAchievement ua where ua.user.id = :userId ORDER BY ua.earnedAt DESC LIMIT 1")
    Optional<UserAchievement> findFirstByUserId(@Param("userId") Long userId);

    @Query("SELECT ua from UserAchievement ua where ua.achievement.id = :achievementId ORDER BY ua.earnedAt DESC limit 1")
    Optional<UserAchievement> findFirstByAchievementId(@Param("achievementId") Long achievementId);
}
