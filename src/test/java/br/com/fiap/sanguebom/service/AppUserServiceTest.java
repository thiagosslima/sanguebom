package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.exception.UserAlreadyExistsException;
import br.com.fiap.sanguebom.model.dtos.AppUserDTO;
import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserService appUserService;

    private AppUser entity() {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setCpfHash("hash-abc");
        u.setName("Maria");
        u.setBirthDate(LocalDate.of(1990, 5, 20));
        u.setEmail("maria@example.com");
        u.setStatus("ACTIVE");
        u.setCreatedAt(NOW);
        u.setUpdatedAt(NOW);
        return u;
    }

    private AppUserDTO dto() {
        AppUserDTO d = new AppUserDTO();
        d.setCpfHash("hash-xyz");
        d.setName("João");
        d.setBirthDate(LocalDate.of(1985, 1, 2));
        d.setEmail("joao@example.com");
        d.setStatus("ACTIVE");
        d.setCreatedAt(NOW);
        d.setUpdatedAt(NOW);
        return d;
    }

    @Test
    @DisplayName("findAll ordena por id e mapeia todos os campos para o DTO")
    void findAllMapsEveryField() {
        when(appUserRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        List<AppUserDTO> result = appUserService.findAll();

        assertThat(result).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getCpfHash()).isEqualTo("hash-abc");
            assertThat(d.getName()).isEqualTo("Maria");
            assertThat(d.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 20));
            assertThat(d.getEmail()).isEqualTo("maria@example.com");
            assertThat(d.getStatus()).isEqualTo("ACTIVE");
            assertThat(d.getCreatedAt()).isEqualTo(NOW);
            assertThat(d.getUpdatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("findAll devolve lista vazia quando não há usuários")
    void findAllHandlesEmptyRepository() {
        when(appUserRepository.findAll(Sort.by("id"))).thenReturn(List.of());
        assertThat(appUserService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("get devolve o usuário existente")
    void getReturnsExistingUser() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(appUserService.get(1L).getName()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("get lança NotFoundException para id inexistente")
    void getThrowsWhenMissing() {
        when(appUserRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> appUserService.get(404L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create persiste os campos vindos do DTO e devolve o id gerado")
    void createPersistsMappedEntity() {
        AppUser saved = entity();
        saved.setId(77L);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(saved);

        Long id = appUserService.create(dto());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());

        assertThat(id).isEqualTo(77L);
        assertThat(captor.getValue().getName()).isEqualTo("João");
        assertThat(captor.getValue().getEmail()).isEqualTo("joao@example.com");
        assertThat(captor.getValue().getCpfHash()).isEqualTo("hash-xyz");
    }

    @Test
    @DisplayName("update aplica o DTO sobre a entidade existente, preservando o id")
    void updateMutatesExistingEntity() {
        AppUser existing = entity();
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(existing));

        appUserService.update(1L, dto());

        verify(appUserRepository).save(existing);
        assertThat(existing.getId()).isEqualTo(1L);
        assertThat(existing.getName()).isEqualTo("João");
        assertThat(existing.getEmail()).isEqualTo("joao@example.com");
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva nada quando o id não existe")
    void updateThrowsAndSavesNothingWhenMissing() {
        when(appUserRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.update(404L, dto()))
                .isInstanceOf(NotFoundException.class);

        verify(appUserRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ unicidade
    //
    // O cpf_hash e a unica identificacao do cidadao: duplicatas fragmentam o historico
    // clinico entre registros diferentes.

    @Test
    @DisplayName("create recusa um CPF ja cadastrado")
    void createRejectsDuplicatedCpfHash() {
        when(appUserRepository.existsByCpfHash("hash-xyz")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.create(dto()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("CPF");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("create recusa um e-mail ja cadastrado")
    void createRejectsDuplicatedEmail() {
        when(appUserRepository.existsByCpfHash("hash-xyz")).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCase("joao@example.com")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.create(dto()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("joao@example.com");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("update exclui o proprio cidadao da checagem de unicidade")
    void updateIgnoresOwnIdentifiers() {
        AppUser existing = entity();
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appUserRepository.existsByCpfHashAndIdNot("hash-xyz", 1L)).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCaseAndIdNot("joao@example.com", 1L))
                .thenReturn(false);

        appUserService.update(1L, dto());

        verify(appUserRepository).save(existing);
    }

    @Test
    @DisplayName("update recusa o e-mail de outro cidadao")
    void updateRejectsEmailOfAnotherCitizen() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(entity()));
        when(appUserRepository.existsByCpfHashAndIdNot("hash-xyz", 1L)).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCaseAndIdNot("joao@example.com", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> appUserService.update(1L, dto()))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("cidadao sem CPF e sem e-mail nao dispara checagem de unicidade")
    void skipsUniquenessWhenIdentifiersAreAbsent() {
        AppUserDTO semIdentificadores = dto();
        semIdentificadores.setCpfHash(null);
        semIdentificadores.setEmail(null);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(entity());

        appUserService.create(semIdentificadores);

        verify(appUserRepository, never()).existsByCpfHash(any());
        verify(appUserRepository, never()).existsByEmailIgnoreCase(any());
    }
}
