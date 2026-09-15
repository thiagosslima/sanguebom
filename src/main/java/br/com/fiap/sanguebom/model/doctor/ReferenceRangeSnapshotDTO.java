package br.com.fiap.sanguebom.model.doctor;

import br.com.fiap.sanguebom.model.enums.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReferenceRangeSnapshotDTO(
        Long id,
        Sex sex,
        BigDecimal ageMinYears,
        BigDecimal ageMaxYears,
        String version,
        String source,
        LocalDate validFrom,
        LocalDate validUntil
) {
}
