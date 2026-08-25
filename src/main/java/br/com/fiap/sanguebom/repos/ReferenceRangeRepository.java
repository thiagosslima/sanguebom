package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ReferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, Long> {
}
