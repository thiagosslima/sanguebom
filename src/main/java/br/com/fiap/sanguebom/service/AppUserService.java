package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.dtos.AppUserDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;

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
