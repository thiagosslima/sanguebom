package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.Achievement;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByCode(AchivementCode code);
}
