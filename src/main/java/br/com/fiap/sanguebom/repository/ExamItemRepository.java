package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ExamItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamItemRepository extends JpaRepository<ExamItem, Long> {

    @Query("SELECT e FROM ExamItem e WHERE e.id = :id AND e.active = true")
    Optional<ExamItem> findActiveById(Long id);
}
