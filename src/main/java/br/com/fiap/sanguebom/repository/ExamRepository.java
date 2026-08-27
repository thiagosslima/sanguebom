package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
}
