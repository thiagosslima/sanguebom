package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.HealthUnit;
import br.com.fiap.sanguebom.model.HealthUnitDTO;
import br.com.fiap.sanguebom.repos.HealthUnitRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class HealthUnitService {

    private final HealthUnitRepository healthUnitRepository;

    public List<HealthUnitDTO> findAll() {
        final List<HealthUnit> healthUnits = healthUnitRepository.findAll(Sort.by("id"));
        return healthUnits.stream()
                .map(healthUnit -> mapToDTO(healthUnit, new HealthUnitDTO()))
                .toList();
    }

    public HealthUnitDTO get(final Long id) {
        return healthUnitRepository.findById(id)
                .map(healthUnit -> mapToDTO(healthUnit, new HealthUnitDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final HealthUnitDTO healthUnitDTO) {
        final HealthUnit healthUnit = new HealthUnit();
        mapToEntity(healthUnitDTO, healthUnit);
        return healthUnitRepository.save(healthUnit).getId();
    }

    public void update(final Long id, final HealthUnitDTO healthUnitDTO) {
        final HealthUnit healthUnit = healthUnitRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(healthUnitDTO, healthUnit);
        healthUnitRepository.save(healthUnit);
    }

    private HealthUnitDTO mapToDTO(final HealthUnit healthUnit, final HealthUnitDTO healthUnitDTO) {
        healthUnitDTO.setId(healthUnit.getId());
        healthUnitDTO.setName(healthUnit.getName());
        healthUnitDTO.setCnes(healthUnit.getCnes());
        healthUnitDTO.setType(healthUnit.getType());
        healthUnitDTO.setStatus(healthUnit.getStatus());
        healthUnitDTO.setCreatedAt(healthUnit.getCreatedAt());
        return healthUnitDTO;
    }

    private HealthUnit mapToEntity(final HealthUnitDTO healthUnitDTO, final HealthUnit healthUnit) {
        healthUnit.setName(healthUnitDTO.getName());
        healthUnit.setCnes(healthUnitDTO.getCnes());
        healthUnit.setType(healthUnitDTO.getType());
        healthUnit.setStatus(healthUnitDTO.getStatus());
        healthUnit.setCreatedAt(healthUnitDTO.getCreatedAt());
        return healthUnit;
    }
}
