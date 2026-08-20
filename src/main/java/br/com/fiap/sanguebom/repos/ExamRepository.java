package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ExamRepository extends JpaRepository<Exam, Long> {
}
