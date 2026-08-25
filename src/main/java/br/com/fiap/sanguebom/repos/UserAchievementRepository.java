package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
}
