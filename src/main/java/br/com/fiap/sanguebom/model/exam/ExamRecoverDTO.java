package br.com.fiap.sanguebom.model.exam;

import br.com.fiap.sanguebom.model.enums.ExamStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class ExamRecoverDTO {

    private Long id;

    private OffsetDateTime collectedAt;

    private OffsetDateTime releasedAt;

    private ExamStatus status;

    private String externalReference;

    private Long userId;

    private Long healthUnitId;

}
