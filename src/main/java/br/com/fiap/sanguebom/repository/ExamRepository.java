package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findFirstByUserIdOrderByCollectedAtDesc(Long userId);

    @EntityGraph(attributePaths = "healthUnit")
    Page<Exam> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"healthUnit", "riskAssessment"})
    Optional<Exam> findByIdAndUserId(Long id, Long userId);
}
