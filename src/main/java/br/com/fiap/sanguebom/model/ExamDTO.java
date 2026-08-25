package br.com.fiap.sanguebom.model;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class ExamDTO {

    private Long id;

    private OffsetDateTime collectedAt;

    private OffsetDateTime releasedAt;

    @Size(max = 30)
    private String status;

    @Size(max = 100)
    private String externalReference;

    private Long user;

    private Long healthUnit;

}
