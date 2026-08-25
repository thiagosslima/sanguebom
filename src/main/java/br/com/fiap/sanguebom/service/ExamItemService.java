package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.ExamItem;
import br.com.fiap.sanguebom.model.ExamItemDTO;
import br.com.fiap.sanguebom.repos.ExamItemRepository;
import br.com.fiap.sanguebom.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ExamItemService {

    private final ExamItemRepository examItemRepository;
    private final ApplicationEventPublisher publisher;

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

}
