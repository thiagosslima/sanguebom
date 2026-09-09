package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.enums.Sex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, Long> {

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
            @Param("sex") Sex sex,
            @Param("age") long age
    );

    @EntityGraph(attributePaths = "referenceRangeRules")
    @Query("""
            SELECT rr FROM ReferenceRange rr
            WHERE rr.examItem.id = :examItemId
            AND (rr.validFrom IS NULL OR rr.validFrom <= :currentDate)
            AND (rr.validUntil IS NULL OR rr.validUntil >= :currentDate)
            ORDER BY rr.sex ASC NULLS FIRST, rr.ageMinYears ASC NULLS FIRST, rr.ageMaxYears ASC NULLS LAST, rr.version DESC
            """)
    List<ReferenceRange> findCurrentRangesByExamItemId(
            @Param("examItemId") Long examItemId,
            @Param("currentDate") LocalDate currentDate
    );
}
