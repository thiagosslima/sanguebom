package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
}
