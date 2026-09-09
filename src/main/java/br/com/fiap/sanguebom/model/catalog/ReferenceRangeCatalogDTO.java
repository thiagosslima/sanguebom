package br.com.fiap.sanguebom.model.catalog;

import br.com.fiap.sanguebom.model.enums.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReferenceRangeCatalogDTO(
        Long id,
        Sex sex,
        BigDecimal ageMinYears,
        BigDecimal ageMaxYears,
        String version,
        String source,
        LocalDate validFrom,
        LocalDate validUntil,
        List<ReferenceRuleCatalogDTO> rules
) {
}
