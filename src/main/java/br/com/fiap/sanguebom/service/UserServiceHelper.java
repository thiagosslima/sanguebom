package br.com.fiap.sanguebom.service;


import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.repository.AppUserRepository;
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
