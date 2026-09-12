package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findFirstByUserIdOrderByCollectedAtDesc(Long userId);

    @EntityGraph(attributePaths = "healthUnit")
    Page<Exam> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"healthUnit", "riskAssessment"})
    Optional<Exam> findByIdAndUserId(Long id, Long userId);

    void updateExamStatusById(Long id, ExamStatus status);

    @Query("SELECT COUNT(e) FROM Exam e WHERE e.user.id = :userId AND e.status = :status")
    long countByStatusAndUserId(Long userId, ExamStatus status);

    @Query("""
        SELECT e FROM Exam e 
            WHERE e.user.id = :userId AND e.status = br.com.fiap.sanguebom.model.enums.ExamStatus.RELEASED
        ORDER BY e.releasedAt DESC
                """)
    List<Exam> findLatestReleasedByUserId(Long userId);
}
