package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ExamItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamItemRepository extends JpaRepository<ExamItem, Long> {
}
