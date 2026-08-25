package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
}
