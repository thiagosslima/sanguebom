package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.HealthProfile;
import br.com.fiap.sanguebom.model.dtos.HealthProfileDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.HealthProfileRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final HealthProfileRepository healthProfileRepository;
    private final AppUserRepository appUserRepository;

    public List<HealthProfileDTO> findAll() {
        final List<HealthProfile> healthProfiles = healthProfileRepository.findAll(Sort.by("id"));
        return healthProfiles.stream()
                .map(healthProfile -> mapToDTO(healthProfile, new HealthProfileDTO()))
                .toList();
    }

    public HealthProfileDTO get(final Long id) {
        return healthProfileRepository.findById(id)
                .map(healthProfile -> mapToDTO(healthProfile, new HealthProfileDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public HealthProfile getByUserId(final Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Perfil de saúde não encontrado para o usuário: %d", userId)));
    }

    public Long create(final HealthProfileDTO healthProfileDTO) {
        final HealthProfile healthProfile = new HealthProfile();
        mapToEntity(healthProfileDTO, healthProfile);
        return healthProfileRepository.save(healthProfile).getId();
    }

    public void update(final Long id, final HealthProfileDTO healthProfileDTO) {
        final HealthProfile healthProfile = healthProfileRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(healthProfileDTO, healthProfile);
        healthProfileRepository.save(healthProfile);
    }

    private HealthProfileDTO mapToDTO(final HealthProfile healthProfile,
                                      final HealthProfileDTO healthProfileDTO) {
        healthProfileDTO.setId(healthProfile.getId());
        healthProfileDTO.setSex(healthProfile.getSex());
        healthProfileDTO.setHeightCm(healthProfile.getHeightCm());
        healthProfileDTO.setWeightKg(healthProfile.getWeightKg());
        healthProfileDTO.setRiskFactors(healthProfile.getRiskFactors());
        healthProfileDTO.setExamPeriodicity(healthProfile.getExamPeriodicity());
        healthProfileDTO.setCreatedAt(healthProfile.getCreatedAt());
        healthProfileDTO.setUpdatedAt(healthProfile.getUpdatedAt());
        healthProfileDTO.setUser(healthProfile.getUser() == null ? null : healthProfile.getUser().getId());
        return healthProfileDTO;
    }

    private HealthProfile mapToEntity(final HealthProfileDTO healthProfileDTO,
                                      final HealthProfile healthProfile) {
        healthProfile.setSex(healthProfileDTO.getSex());
        healthProfile.setHeightCm(healthProfileDTO.getHeightCm());
        healthProfile.setWeightKg(healthProfileDTO.getWeightKg());
        healthProfile.setRiskFactors(healthProfileDTO.getRiskFactors());
        healthProfile.setExamPeriodicity(healthProfileDTO.getExamPeriodicity());
        healthProfile.setCreatedAt(healthProfileDTO.getCreatedAt());
        healthProfile.setUpdatedAt(healthProfileDTO.getUpdatedAt());
        final AppUser user = healthProfileDTO.getUser() == null ? null : appUserRepository.findById(healthProfileDTO.getUser())
                .orElseThrow(() -> new NotFoundException("user not found"));
        healthProfile.setUser(user);
        return healthProfile;
    }
}
