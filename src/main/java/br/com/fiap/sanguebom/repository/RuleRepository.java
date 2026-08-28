package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    List<Rule> findByReferenceRangeId(Long referenceRangeId);
}
