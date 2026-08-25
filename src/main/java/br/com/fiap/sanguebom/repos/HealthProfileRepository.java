package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {
}
