package br.com.fiap.sanguebom.model.dtos;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class ExamItemDTO {

    private Long id;

    @Size(max = 50)
    private String code;

    @Size(max = 150)
    private String name;

    @Size(max = 50)
    private String unit;

    @Size(max = 50)
    private String category;

    @Size(max = 500)
    private String description;

    private Boolean active;

    private OffsetDateTime createdAt;

}
