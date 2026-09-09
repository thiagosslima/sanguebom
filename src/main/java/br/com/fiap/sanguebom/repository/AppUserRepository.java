package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.AppUser;
import jakarta.persistence.Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Page<AppUser> findByStatusIgnoreCase(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"healthProfile", "userAchievements", "userAchievements.achievement"})
    Optional<AppUser> findById(String email);
}
