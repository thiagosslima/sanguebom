package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ReferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, Long> {

    @Query("select rf from ReferenceRange rf where rf.examItem.id = :examItemId order by rf.lastUpdated desc limit 1")
    Optional<ReferenceRange> findFirstByExamItemId(@Param("examItemId")Long examItemId);
}
