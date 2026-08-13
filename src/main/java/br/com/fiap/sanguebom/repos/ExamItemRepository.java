package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ExamItem;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ExamItemRepository extends JpaRepository<ExamItem, Long> {
}
