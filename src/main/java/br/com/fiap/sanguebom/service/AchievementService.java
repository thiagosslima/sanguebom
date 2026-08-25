package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.Achievement;
import br.com.fiap.sanguebom.model.AchievementDTO;
import br.com.fiap.sanguebom.repos.AchievementRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;

    public List<AchievementDTO> findAll() {
        final List<Achievement> achievements = achievementRepository.findAll(Sort.by("id"));
        return achievements.stream()
                .map(achievement -> mapToDTO(achievement, new AchievementDTO()))
                .toList();
    }

    public AchievementDTO get(final Long id) {
        return achievementRepository.findById(id)
                .map(achievement -> mapToDTO(achievement, new AchievementDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final AchievementDTO achievementDTO) {
        final Achievement achievement = new Achievement();
        mapToEntity(achievementDTO, achievement);
        return achievementRepository.save(achievement).getId();
    }

    public void update(final Long id, final AchievementDTO achievementDTO) {
        final Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(achievementDTO, achievement);
        achievementRepository.save(achievement);
    }

    private AchievementDTO mapToDTO(final Achievement achievement,
                                    final AchievementDTO achievementDTO) {
        achievementDTO.setId(achievement.getId());
        achievementDTO.setCode(achievement.getCode());
        achievementDTO.setName(achievement.getName());
        achievementDTO.setDescription(achievement.getDescription());
        achievementDTO.setActive(achievement.getActive());
        achievementDTO.setCreatedAt(achievement.getCreatedAt());
        return achievementDTO;
    }

    private Achievement mapToEntity(final AchievementDTO achievementDTO,
                                    final Achievement achievement) {
        achievement.setCode(achievementDTO.getCode());
        achievement.setName(achievementDTO.getName());
        achievement.setDescription(achievementDTO.getDescription());
        achievement.setActive(achievementDTO.getActive());
        return achievement;
    }

}
