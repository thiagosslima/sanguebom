package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
}
