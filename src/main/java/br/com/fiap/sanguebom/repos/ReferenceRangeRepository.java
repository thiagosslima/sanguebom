package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ReferenceRange;
import br.com.fiap.sanguebom.model.enums.AppUserSex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, Long> {

    @Query("select rf from ReferenceRange rf where rf.examItem.id = :examItemId order by rf.lastUpdated desc limit 1")
    Optional<ReferenceRange> findFirstByExamItemId(@Param("examItemId")Long examItemId);


    @Query("""
        SELECT rr from ReferenceRange  rr
        WHERE rr.examItem.id = :examItemId
        AND (
            rr.sex is NULL or rr.sex = :sex
        )
        AND (
            rr.ageMinYears is NULL or rr.ageMinYears <= :age
        )
        AND (
            rr.ageMaxYears is NULL or rr.ageMaxYears >= :age
        )
""")
    Optional<ReferenceRange> findApplicableRangeForUserByExamItem(
            @Param("examItemId") Long examItemId,
            @Param("sex") AppUserSex sex,
            @Param("age") long age
    );
}
