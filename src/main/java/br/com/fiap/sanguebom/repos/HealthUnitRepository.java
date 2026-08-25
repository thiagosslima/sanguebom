package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.HealthUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthUnitRepository extends JpaRepository<HealthUnit, Long> {
}
