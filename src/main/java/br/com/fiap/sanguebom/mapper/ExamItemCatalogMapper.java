package br.com.fiap.sanguebom.mapper;

import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRuleCatalogDTO;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.entities.Rule;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class ExamItemCatalogMapper {

    public ExamItemCatalogDTO toCatalogDTO(final ExamItem examItem) {
        return new ExamItemCatalogDTO(
                examItem.getId(),
                examItem.getCode(),
                examItem.getName(),
                examItem.getUnit(),
                examItem.getCategory(),
                examItem.getDescription());
    }

    public ReferenceRangeCatalogDTO toReferenceRangeCatalogDTO(final ReferenceRange referenceRange) {
        return new ReferenceRangeCatalogDTO(
                referenceRange.getId(),
                referenceRange.getSex(),
                referenceRange.getAgeMinYears(),
                referenceRange.getAgeMaxYears(),
                referenceRange.getVersion(),
                referenceRange.getSource(),
                referenceRange.getValidFrom(),
                referenceRange.getValidUntil(),
                referenceRange.getReferenceRangeRules().stream()
                        .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                        .sorted(Comparator.comparing(Rule::getMinValue,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .map(this::toReferenceRuleCatalogDTO)
                        .toList());
    }

    private ReferenceRuleCatalogDTO toReferenceRuleCatalogDTO(final Rule rule) {
        return new ReferenceRuleCatalogDTO(
                rule.getId(),
                rule.getMinValue(),
                rule.getMaxValue(),
                rule.getMinInclusive(),
                rule.getMaxInclusive(),
                rule.getLevel(),
                rule.getScore(),
                rule.getDescription(),
                rule.getVersion());
    }
}
