package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.Rule;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RuleRepository extends JpaRepository<Rule, Long> {
}
