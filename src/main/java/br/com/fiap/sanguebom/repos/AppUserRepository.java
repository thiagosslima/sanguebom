package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}
