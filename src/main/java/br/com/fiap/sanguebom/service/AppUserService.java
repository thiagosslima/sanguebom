package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.dtos.AppUserDTO;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.exception.UserAlreadyExistsException;
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
        validateUniqueness(appUserDTO, null);

        final AppUser appUser = new AppUser();
        mapToEntity(appUserDTO, appUser);
        return appUserRepository.save(appUser).getId();
    }

    /**
     * O cpf_hash e a unica identificacao do cidadao: permitir duplicatas fragmenta o historico
     * clinico entre registros diferentes. No update, o proprio cidadao e excluido da checagem.
     */
    private void validateUniqueness(final AppUserDTO appUserDTO, final Long currentId) {
        final String cpfHash = appUserDTO.getCpfHash();
        if (cpfHash != null && cpfHashTaken(cpfHash, currentId)) {
            throw new UserAlreadyExistsException("Já existe um cidadão cadastrado com este CPF");
        }

        final String email = appUserDTO.getEmail();
        if (email != null && emailTaken(email, currentId)) {
            throw new UserAlreadyExistsException(
                    String.format("Já existe um cidadão cadastrado com o e-mail %s", email));
        }
    }

    private boolean cpfHashTaken(final String cpfHash, final Long currentId) {
        return currentId == null
                ? appUserRepository.existsByCpfHash(cpfHash)
                : appUserRepository.existsByCpfHashAndIdNot(cpfHash, currentId);
    }

    private boolean emailTaken(final String email, final Long currentId) {
        return currentId == null
                ? appUserRepository.existsByEmailIgnoreCase(email)
                : appUserRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
    }

    public void update(final Long id, final AppUserDTO appUserDTO) {
        final AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        validateUniqueness(appUserDTO, id);
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
