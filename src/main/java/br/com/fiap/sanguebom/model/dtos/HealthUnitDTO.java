package br.com.fiap.sanguebom.model.dtos;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class HealthUnitDTO {

    private Long id;

    @Size(max = 200)
    private String name;

    @Size(max = 20)
    private String cnes;

    @Size(max = 30)
    private String type;

    @Size(max = 20)
    private String status;

    private OffsetDateTime createdAt;

}
