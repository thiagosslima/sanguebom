package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findFirstByUserIdOrderByCollectedAtDesc(Long userId);

    @EntityGraph(attributePaths = "healthUnit")
    Page<Exam> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"healthUnit", "riskAssessment"})
    Optional<Exam> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            LEFT JOIN FETCH e.examResults r
            LEFT JOIN FETCH r.examItem
            WHERE e.user.id = :userId
            AND e.id IN :examIds
            """)
    List<Exam> findByUserIdAndIdInWithResults(
            @Param("userId") Long userId,
            @Param("examIds") Collection<Long> examIds);

    long countByUserIdAndStatus(Long userId, ExamStatus status);

    List<Exam> findTop2ByUserIdAndStatusOrderByCollectedAtDesc(Long userId, ExamStatus status);
}
