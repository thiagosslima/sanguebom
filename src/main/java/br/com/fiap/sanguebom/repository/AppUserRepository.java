package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Page<AppUser> findByStatusIgnoreCase(String status, Pageable pageable);

    boolean existsByCpfHash(String cpfHash);

    boolean existsByCpfHashAndIdNot(String cpfHash, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
