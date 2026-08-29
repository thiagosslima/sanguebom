package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    @Query("select r from ExamResult r join fetch r.examItem item "
            + "where r.exam.id = :examId order by item.name")
    List<ExamResult> findByExamIdWithItem(@Param("examId") Long examId);
}
