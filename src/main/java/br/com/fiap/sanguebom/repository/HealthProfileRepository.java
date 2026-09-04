package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {

    Optional<HealthProfile> findByUserId(Long userId);
}
