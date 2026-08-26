package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface RuleRepository extends JpaRepository<Rule, Long> {

    @Query("select ru from Rule ru where ru.referenceRange.id = :refRangeId order by ru.lastUpdated desc limit 1")
    Optional<Rule> findFirstByReferenceRangeId(@Param("refRangeId")Long referenceRangeId);

    List<Rule> findByReferenceRangeId(Long referenceRangeId);
}
