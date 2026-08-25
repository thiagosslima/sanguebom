package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ExamItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface ExamItemRepository extends JpaRepository<ExamItem, Long> {

    @Query("SELECT e FROM ExamItem e WHERE e.id = :id AND e.active = true")
    Optional<ExamItem> findActiveById(Long id);
}
