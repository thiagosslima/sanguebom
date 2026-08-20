package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.AppUser;
import br.com.fiap.sanguebom.events.BeforeDeleteAppUser;
import br.com.fiap.sanguebom.model.AppUserDTO;
import br.com.fiap.sanguebom.repos.AppUserRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final ApplicationEventPublisher publisher;

    public AppUserService(final AppUserRepository appUserRepository,
            final ApplicationEventPublisher publisher) {
        this.appUserRepository = appUserRepository;
        this.publisher = publisher;
    }

    public List<AppUserDTO> findAll() {
        final List<AppUser> appUsers = appUserRepository.findAll(Sort.by("id"));
        return appUsers.stream()
                .map(appUser -> mapToDTO(appUser, new AppUserDTO()))
                .toList();
    }

    public AppUserDTO get(final Long id) {
        return appUserRepository.findById(id)
                .map(appUser -> mapToDTO(appUser, new AppUserDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final AppUserDTO appUserDTO) {
        final AppUser appUser = new AppUser();
        mapToEntity(appUserDTO, appUser);
        return appUserRepository.save(appUser).getId();
    }

    public void update(final Long id, final AppUserDTO appUserDTO) {
        final AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(appUserDTO, appUser);
        appUserRepository.save(appUser);
    }

    public void delete(final Long id) {
        final AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteAppUser(id));
        appUserRepository.delete(appUser);
    }

    private AppUserDTO mapToDTO(final AppUser appUser, final AppUserDTO appUserDTO) {
        appUserDTO.setId(appUser.getId());
        appUserDTO.setCpfHash(appUser.getCpfHash());
        appUserDTO.setName(appUser.getName());
        appUserDTO.setBirthDate(appUser.getBirthDate());
        appUserDTO.setEmail(appUser.getEmail());
        appUserDTO.setStatus(appUser.getStatus());
        appUserDTO.setCreatedAt(appUser.getCreatedAt());
        appUserDTO.setUpdatedAt(appUser.getUpdatedAt());
        return appUserDTO;
    }

    private AppUser mapToEntity(final AppUserDTO appUserDTO, final AppUser appUser) {
        appUser.setCpfHash(appUserDTO.getCpfHash());
        appUser.setName(appUserDTO.getName());
        appUser.setBirthDate(appUserDTO.getBirthDate());
        appUser.setEmail(appUserDTO.getEmail());
        appUser.setStatus(appUserDTO.getStatus());
        appUser.setCreatedAt(appUserDTO.getCreatedAt());
        appUser.setUpdatedAt(appUserDTO.getUpdatedAt());
        return appUser;
    }

}
