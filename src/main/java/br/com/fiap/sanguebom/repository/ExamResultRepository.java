package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ExamResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    @Query("select r from ExamResult r join fetch r.examItem item "
            + "where r.exam.id = :examId order by item.name")
    List<ExamResult> findByExamIdWithItem(@Param("examId") Long examId);

    @EntityGraph(attributePaths = {"exam", "examItem"})
    @Query(value = """
            SELECT r FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            ORDER BY e.collectedAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            """)
    Page<ExamResult> findTimelineByUserAndItemCode(
            @Param("userId") Long userId,
            @Param("itemCode") String itemCode,
            Pageable pageable);

    @EntityGraph(attributePaths = {"exam", "examItem"})
    @Query(value = """
            SELECT r FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            AND e.collectedAt >= :fromAt
            ORDER BY e.collectedAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            AND e.collectedAt >= :fromAt
            """)
    Page<ExamResult> findTimelineByUserAndItemCodeFrom(
            @Param("userId") Long userId,
            @Param("itemCode") String itemCode,
            @Param("fromAt") OffsetDateTime fromAt,
            Pageable pageable);

    @EntityGraph(attributePaths = {"exam", "examItem"})
    @Query(value = """
            SELECT r FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            AND e.collectedAt < :toExclusive
            ORDER BY e.collectedAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            AND e.collectedAt < :toExclusive
            """)
    Page<ExamResult> findTimelineByUserAndItemCodeUntil(
            @Param("userId") Long userId,
            @Param("itemCode") String itemCode,
            @Param("toExclusive") OffsetDateTime toExclusive,
            Pageable pageable);

    @EntityGraph(attributePaths = {"exam", "examItem"})
    @Query(value = """
            SELECT r FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            AND e.collectedAt >= :fromAt
            AND e.collectedAt < :toExclusive
            ORDER BY e.collectedAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM ExamResult r
            JOIN r.exam e
            JOIN r.examItem item
            WHERE e.user.id = :userId
            AND UPPER(item.code) = UPPER(:itemCode)
            AND e.collectedAt IS NOT NULL
            AND e.collectedAt >= :fromAt
            AND e.collectedAt < :toExclusive
            """)
    Page<ExamResult> findTimelineByUserAndItemCodeBetween(
            @Param("userId") Long userId,
            @Param("itemCode") String itemCode,
            @Param("fromAt") OffsetDateTime fromAt,
            @Param("toExclusive") OffsetDateTime toExclusive,
            Pageable pageable);
}
