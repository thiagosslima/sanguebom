package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ExamItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamItemRepository extends JpaRepository<ExamItem, Long> {

    @Query("SELECT e FROM ExamItem e WHERE e.id = :id AND e.active = true")
    Optional<ExamItem> findActiveById(@Param("id") Long id);

    @Query("""
            SELECT e FROM ExamItem e
            WHERE e.active = true
            """)
    Page<ExamItem> findActiveCatalogItems(Pageable pageable);

    @Query("""
            SELECT e FROM ExamItem e
            WHERE e.active = true
            AND UPPER(e.category) = UPPER(:category)
            """)
    Page<ExamItem> findActiveCatalogItemsByCategory(@Param("category") String category, Pageable pageable);

    @Query("""
            SELECT e FROM ExamItem e
            WHERE e.active = true
            AND UPPER(e.code) = UPPER(:code)
            """)
    Optional<ExamItem> findActiveByCode(@Param("code") String code);
}
