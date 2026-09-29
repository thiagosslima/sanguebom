package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long>, JpaSpecificationExecutor<RiskAssessment> {
}
