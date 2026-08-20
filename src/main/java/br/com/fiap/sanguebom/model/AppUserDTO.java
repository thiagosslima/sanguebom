package br.com.fiap.sanguebom.model;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;


@Getter
@Setter
public class AppUserDTO {

    private Long id;

    @Size(max = 128)
    private String cpfHash;

    @Size(max = 150)
    private String name;

    private LocalDate birthDate;

    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

}
