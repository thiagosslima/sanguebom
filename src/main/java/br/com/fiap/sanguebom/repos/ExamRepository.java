package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT ex FROM Exam ex where ex.user.id = :userId ORDER BY ex.collectedAt DESC limit 1")
    Optional<Exam> findFirstByUserId(@Param("userId") Long userId);

    @Query("select ex from Exam ex where ex.healthUnit.id = :healthUnitId ORDER BY ex.collectedAt desc limit 1")
    Optional<Exam> findFirstByHealthUnitId(@Param("healthUnitId")Long healthUnitId);
}
