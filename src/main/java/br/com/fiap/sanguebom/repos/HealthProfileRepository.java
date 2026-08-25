package br.com.fiap.sanguebom.repos;

import br.com.fiap.sanguebom.domain.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {

    @Query("select hp from HealthProfile hp where  hp.user.id = :userId order by hp.lastUpdated desc limit 1")
    Optional<HealthProfile> findFirstByUserId(@Param("userId")Long userId);
}
