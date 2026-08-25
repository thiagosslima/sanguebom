package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.Achievement;
import br.com.fiap.sanguebom.domain.AppUser;
import br.com.fiap.sanguebom.domain.UserAchievement;
import br.com.fiap.sanguebom.events.BeforeDeleteAchievement;
import br.com.fiap.sanguebom.events.BeforeDeleteAppUser;
import br.com.fiap.sanguebom.model.UserAchievementDTO;
import br.com.fiap.sanguebom.repos.AchievementRepository;
import br.com.fiap.sanguebom.repos.AppUserRepository;
import br.com.fiap.sanguebom.repos.UserAchievementRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import br.com.fiap.sanguebom.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class UserAchievementService {

    private final UserAchievementRepository userAchievementRepository;
    private final AppUserRepository appUserRepository;
    private final AchievementRepository achievementRepository;

    public UserAchievementService(final UserAchievementRepository userAchievementRepository,
            final AppUserRepository appUserRepository,
            final AchievementRepository achievementRepository) {
        this.userAchievementRepository = userAchievementRepository;
        this.appUserRepository = appUserRepository;
        this.achievementRepository = achievementRepository;
    }

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
        final UserAchievement userAchievement = new UserAchievement();
        mapToEntity(userAchievementDTO, userAchievement);
        return userAchievementRepository.save(userAchievement).getId();
    }

    public void update(final Long id, final UserAchievementDTO userAchievementDTO) {
        final UserAchievement userAchievement = userAchievementRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(userAchievementDTO, userAchievement);
        userAchievementRepository.save(userAchievement);
    }

    public void delete(final Long id) {
        final UserAchievement userAchievement = userAchievementRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        userAchievementRepository.delete(userAchievement);
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

    @EventListener(BeforeDeleteAppUser.class)
    public void on(final BeforeDeleteAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final UserAchievement userUserAchievement = userAchievementRepository.findFirstByUserId(event.getId()).orElse(null);
        if (userUserAchievement != null) {
            referencedException.setKey("appUser.userAchievement.user.referenced");
            referencedException.addParam(userUserAchievement.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteAchievement.class)
    public void on(final BeforeDeleteAchievement event) {
        final ReferencedException referencedException = new ReferencedException();
        final UserAchievement achievementUserAchievement = userAchievementRepository.findFirstByAchievementId(event.getId()).orElse(null);
        if (achievementUserAchievement != null) {
            referencedException.setKey("achievement.userAchievement.achievement.referenced");
            referencedException.addParam(achievementUserAchievement.getId());
            throw referencedException;
        }
    }

}
