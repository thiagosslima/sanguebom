package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.BloodPressure;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BloodPressureRepository extends JpaRepository<BloodPressure, Long> {
}
