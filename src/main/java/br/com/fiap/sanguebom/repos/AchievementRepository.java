package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AchievementRepository extends JpaRepository<Achievement, Long> {
}
