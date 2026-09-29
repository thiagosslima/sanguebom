package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.mapper.ExamItemCatalogMapper;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.dtos.ExamItemDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.repository.ReferenceRangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ExamItemService {

    private final ExamItemRepository examItemRepository;
    private final ReferenceRangeRepository referenceRangeRepository;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;
    private final ExamItemCatalogMapper examItemCatalogMapper;

    @Transactional(readOnly = true)
    public PageResponse<ExamItemCatalogDTO> findActiveCatalogItems(
            final String category,
            final int page,
            final int size) {
        final var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        final var items = blankToNull(category) == null
                ? examItemRepository.findActiveCatalogItems(pageable)
                : examItemRepository.findActiveCatalogItemsByCategory(category.trim(), pageable);

        return PageResponse.of(items.map(examItemCatalogMapper::toCatalogDTO));
    }

    @Transactional(readOnly = true)
    public List<ReferenceRangeCatalogDTO> findCurrentReferenceRanges(final String itemCode) {
        final ExamItem examItem = examItemRepository.findActiveByCode(itemCode)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Item de exame ativo não encontrado para o código: %s", itemCode)));

        return referenceRangeRepository
                .findCurrentRangesByExamItemId(examItem.getId(), LocalDate.now(clock))
                .stream()
                .map(examItemCatalogMapper::toReferenceRangeCatalogDTO)
                .toList();
    }

    public List<ExamItemDTO> findAll() {
        final List<ExamItem> examItems = examItemRepository.findAll(Sort.by("id"));
        return examItems.stream()
                .map(examItem -> mapToDTO(examItem, new ExamItemDTO()))
                .toList();
    }

    public ExamItemDTO get(final Long id) {
        return examItemRepository.findById(id)
                .map(examItem -> mapToDTO(examItem, new ExamItemDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ExamItemDTO examItemDTO) {
        final ExamItem examItem = new ExamItem();
        mapToEntity(examItemDTO, examItem);
        return examItemRepository.save(examItem).getId();
    }

    public void update(final Long id, final ExamItemDTO examItemDTO) {
        final ExamItem examItem = examItemRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(examItemDTO, examItem);
        examItemRepository.save(examItem);
    }

    private ExamItemDTO mapToDTO(final ExamItem examItem, final ExamItemDTO examItemDTO) {
        examItemDTO.setId(examItem.getId());
        examItemDTO.setCode(examItem.getCode());
        examItemDTO.setName(examItem.getName());
        examItemDTO.setUnit(examItem.getUnit());
        examItemDTO.setCategory(examItem.getCategory());
        examItemDTO.setDescription(examItem.getDescription());
        examItemDTO.setActive(examItem.getActive());
        examItemDTO.setCreatedAt(examItem.getCreatedAt());
        return examItemDTO;
    }

    private ExamItem mapToEntity(final ExamItemDTO examItemDTO, final ExamItem examItem) {
        examItem.setCode(examItemDTO.getCode());
        examItem.setName(examItemDTO.getName());
        examItem.setUnit(examItemDTO.getUnit());
        examItem.setCategory(examItemDTO.getCategory());
        examItem.setDescription(examItemDTO.getDescription());
        examItem.setActive(examItemDTO.getActive());
        examItem.setCreatedAt(examItemDTO.getCreatedAt());
        return examItem;
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value;
    }

}
