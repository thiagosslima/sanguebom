package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, Long> {
}
