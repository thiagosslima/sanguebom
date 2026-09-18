package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.Achievement;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.UserAchievement;
import br.com.fiap.sanguebom.model.dtos.UserAchievementDTO;
import br.com.fiap.sanguebom.repository.AchievementRepository;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.repository.UserAchievementRepository;
import br.com.fiap.sanguebom.exception.DuplicatedAchievementException;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserAchievementService {

    private final UserAchievementRepository userAchievementRepository;
    private final AppUserRepository appUserRepository;
    private final AchievementRepository achievementRepository;

    public List<UserAchievementDTO> findAll() {
        final List<UserAchievement> userAchievements = userAchievementRepository.findAll(Sort.by("id"));
        return userAchievements.stream()
                .map(userAchievement -> mapToDTO(userAchievement, new UserAchievementDTO()))
                .toList();
    }

    public UserAchievementDTO get(final Long id) {
        return userAchievementRepository.findById(id)
                .map(userAchievement -> mapToDTO(userAchievement, new UserAchievementDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final UserAchievementDTO userAchievementDTO) {
        validateNotAlreadyGranted(userAchievementDTO, null);

        final UserAchievement userAchievement = new UserAchievement();
        mapToEntity(userAchievementDTO, userAchievement);
        return userAchievementRepository.save(userAchievement).getId();
    }

    public void update(final Long id, final UserAchievementDTO userAchievementDTO) {
        final UserAchievement userAchievement = userAchievementRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        validateNotAlreadyGranted(userAchievementDTO, id);

        mapToEntity(userAchievementDTO, userAchievement);
        userAchievementRepository.save(userAchievement);
    }

    /**
     * O par (user, achievement) e unico: cada cidadao ganha cada conquista uma unica vez. Sem esta
     * checagem, a tentativa esbarraria na constraint do banco e viraria um 500.
     */
    private void validateNotAlreadyGranted(final UserAchievementDTO userAchievementDTO,
                                           final Long currentId) {
        final Long userId = userAchievementDTO.getUser();
        final Long achievementId = userAchievementDTO.getAchievement();
        if (userId == null || achievementId == null) {
            return;
        }

        final boolean granted = currentId == null
                ? userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)
                : userAchievementRepository.existsByUserIdAndAchievementIdAndIdNot(
                        userId, achievementId, currentId);

        if (granted) {
            throw new DuplicatedAchievementException(
                    String.format("O cidadão %d já possui a conquista %d", userId, achievementId));
        }
    }

    private UserAchievementDTO mapToDTO(final UserAchievement userAchievement,
                                        final UserAchievementDTO userAchievementDTO) {
        userAchievementDTO.setId(userAchievement.getId());
        userAchievementDTO.setEarnedAt(userAchievement.getEarnedAt());
        userAchievementDTO.setUser(userAchievement.getUser() == null ? null : userAchievement.getUser().getId());
        userAchievementDTO.setAchievement(userAchievement.getAchievement() == null ? null : userAchievement.getAchievement().getId());
        return userAchievementDTO;
    }

    private UserAchievement mapToEntity(final UserAchievementDTO userAchievementDTO,
                                        final UserAchievement userAchievement) {
        userAchievement.setEarnedAt(userAchievementDTO.getEarnedAt());
        final AppUser user = userAchievementDTO.getUser() == null ? null : appUserRepository.findById(userAchievementDTO.getUser())
                .orElseThrow(() -> new NotFoundException("user not found"));
        userAchievement.setUser(user);
        final Achievement achievement = userAchievementDTO.getAchievement() == null ? null : achievementRepository.findById(userAchievementDTO.getAchievement())
                .orElseThrow(() -> new NotFoundException("achievement not found"));
        userAchievement.setAchievement(achievement);
        return userAchievement;
    }
}
