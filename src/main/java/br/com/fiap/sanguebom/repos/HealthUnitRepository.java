package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.HealthUnit;
import org.springframework.data.jpa.repository.JpaRepository;


public interface HealthUnitRepository extends JpaRepository<HealthUnit, Long> {
}
