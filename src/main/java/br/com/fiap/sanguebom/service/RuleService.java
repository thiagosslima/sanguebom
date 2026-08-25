package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.ReferenceRange;
import br.com.fiap.sanguebom.domain.Rule;
import br.com.fiap.sanguebom.model.RuleDTO;
import br.com.fiap.sanguebom.repos.ReferenceRangeRepository;
import br.com.fiap.sanguebom.repos.RuleRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final ReferenceRangeRepository referenceRangeRepository;

    public List<RuleDTO> findAll() {
        final List<Rule> rules = ruleRepository.findAll(Sort.by("id"));
        return rules.stream()
                .map(rule -> mapToDTO(rule, new RuleDTO()))
                .toList();
    }

    public RuleDTO get(final Long id) {
        return ruleRepository.findById(id)
                .map(rule -> mapToDTO(rule, new RuleDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final RuleDTO ruleDTO) {
        final Rule rule = new Rule();
        mapToEntity(ruleDTO, rule);
        return ruleRepository.save(rule).getId();
    }

    public void update(final Long id, final RuleDTO ruleDTO) {
        final Rule rule = ruleRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(ruleDTO, rule);
        ruleRepository.save(rule);
    }

    private RuleDTO mapToDTO(final Rule rule, final RuleDTO ruleDTO) {
        ruleDTO.setId(rule.getId());
        ruleDTO.setMinValue(rule.getMinValue());
        ruleDTO.setMaxValue(rule.getMaxValue());
        ruleDTO.setMinInclusive(rule.getMinInclusive());
        ruleDTO.setMaxInclusive(rule.getMaxInclusive());
        ruleDTO.setLevel(rule.getLevel());
        ruleDTO.setScore(rule.getScore());
        ruleDTO.setDescription(rule.getDescription());
        ruleDTO.setVersion(rule.getVersion());
        ruleDTO.setActive(rule.getActive());
        ruleDTO.setCreatedAt(rule.getCreatedAt());
        ruleDTO.setReferenceRange(rule.getReferenceRange() == null ? null : rule.getReferenceRange().getId());
        return ruleDTO;
    }

    private Rule mapToEntity(final RuleDTO ruleDTO, final Rule rule) {
        rule.setMinValue(ruleDTO.getMinValue());
        rule.setMaxValue(ruleDTO.getMaxValue());
        rule.setMinInclusive(ruleDTO.getMinInclusive());
        rule.setMaxInclusive(ruleDTO.getMaxInclusive());
        rule.setLevel(ruleDTO.getLevel());
        rule.setScore(ruleDTO.getScore());
        rule.setDescription(ruleDTO.getDescription());
        rule.setVersion(ruleDTO.getVersion());
        rule.setActive(ruleDTO.getActive());
        rule.setCreatedAt(ruleDTO.getCreatedAt());
        final ReferenceRange referenceRange = ruleDTO.getReferenceRange() == null ? null : referenceRangeRepository.findById(ruleDTO.getReferenceRange())
                .orElseThrow(() -> new NotFoundException("referenceRange not found"));
        rule.setReferenceRange(referenceRange);
        return rule;
    }
}
