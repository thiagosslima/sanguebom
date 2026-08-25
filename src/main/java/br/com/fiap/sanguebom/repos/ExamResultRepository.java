package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    @Query("SELECT exres FROM ExamResult exres where exres.exam.id = :examId ORDER BY exres.lastUpdated DESC limit 1")
    Optional<ExamResult> findFirstByExamId(@Param("examId") Long examId);

    @Query("select exres from ExamResult exres where exres.examItem.id = :examItemId ORDER BY exres.lastUpdated desc limit 1")
    Optional<ExamResult> findFirstByExamItemId(@Param("examItemId")Long examItemId);
}
