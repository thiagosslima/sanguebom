package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.AppUser;
import br.com.fiap.sanguebom.repos.AppUserRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceHelper {

    private final AppUserRepository appUserRepository;


    public UserServiceHelper(AppUserRepository appUserRepository){
        this.appUserRepository = appUserRepository;
    }

    public AppUser getUserByIdOrFail(Long id) {
        return appUserRepository.findById(id).orElseThrow(()
                -> new NotFoundException( String.format("Usuário não encontrado para o id: %d", id)));
    }


}
