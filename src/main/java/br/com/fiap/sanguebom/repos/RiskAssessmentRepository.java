package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    @Query("select ra from RiskAssessment ra where ra.user.id = :userId ORDER BY ra.createdAt DESC limit 1")
    Optional<RiskAssessment> findFirstByUserId(@Param("userId") Long userId);

    @Query("select ra from  RiskAssessment  ra where  ra.exam.id = :examId ORDER BY ra.createdAt DESC limit 1")
    Optional<RiskAssessment> findFirstByExamId(@Param("examId") Long examId);
}
