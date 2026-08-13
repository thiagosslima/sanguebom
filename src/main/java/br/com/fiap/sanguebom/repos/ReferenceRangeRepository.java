package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ReferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, Long> {
}
