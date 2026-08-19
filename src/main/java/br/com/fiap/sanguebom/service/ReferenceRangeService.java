package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.domain.ExamItem;
import br.com.fiap.sanguebom.domain.ReferenceRange;
import br.com.fiap.sanguebom.events.BeforeDeleteExamItem;
import br.com.fiap.sanguebom.events.BeforeDeleteReferenceRange;
import br.com.fiap.sanguebom.model.ReferenceRangeDTO;
import br.com.fiap.sanguebom.repos.ExamItemRepository;
import br.com.fiap.sanguebom.repos.ReferenceRangeRepository;
import br.com.fiap.sanguebom.util.NotFoundException;
import br.com.fiap.sanguebom.util.ReferencedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ReferenceRangeService {

    private final ReferenceRangeRepository referenceRangeRepository;
    private final ExamItemRepository examItemRepository;
    private final ApplicationEventPublisher publisher;

    public ReferenceRangeService(final ReferenceRangeRepository referenceRangeRepository,
            final ExamItemRepository examItemRepository,
            final ApplicationEventPublisher publisher) {
        this.referenceRangeRepository = referenceRangeRepository;
        this.examItemRepository = examItemRepository;
        this.publisher = publisher;
    }

    public List<ReferenceRangeDTO> findAll() {
        final List<ReferenceRange> referenceRanges = referenceRangeRepository.findAll(Sort.by("id"));
        return referenceRanges.stream()
                .map(referenceRange -> mapToDTO(referenceRange, new ReferenceRangeDTO()))
                .toList();
    }

    public ReferenceRangeDTO get(final Long id) {
        return referenceRangeRepository.findById(id)
                .map(referenceRange -> mapToDTO(referenceRange, new ReferenceRangeDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ReferenceRangeDTO referenceRangeDTO) {
        final ReferenceRange referenceRange = new ReferenceRange();
        mapToEntity(referenceRangeDTO, referenceRange);
        return referenceRangeRepository.save(referenceRange).getId();
    }

    public void update(final Long id, final ReferenceRangeDTO referenceRangeDTO) {
        final ReferenceRange referenceRange = referenceRangeRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(referenceRangeDTO, referenceRange);
        referenceRangeRepository.save(referenceRange);
    }

    public void delete(final Long id) {
        final ReferenceRange referenceRange = referenceRangeRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteReferenceRange(id));
        referenceRangeRepository.delete(referenceRange);
    }

    private ReferenceRangeDTO mapToDTO(final ReferenceRange referenceRange,
            final ReferenceRangeDTO referenceRangeDTO) {
        referenceRangeDTO.setId(referenceRange.getId());
        referenceRangeDTO.setSex(referenceRange.getSex());
        referenceRangeDTO.setAgeMinYears(referenceRange.getAgeMinYears());
        referenceRangeDTO.setAgeMaxYears(referenceRange.getAgeMaxYears());
        referenceRangeDTO.setVersion(referenceRange.getVersion());
        referenceRangeDTO.setSource(referenceRange.getSource());
        referenceRangeDTO.setValidFrom(referenceRange.getValidFrom());
        referenceRangeDTO.setValidUntil(referenceRange.getValidUntil());
        referenceRangeDTO.setCreatedAt(referenceRange.getCreatedAt());
        referenceRangeDTO.setExamItem(referenceRange.getExamItem() == null ? null : referenceRange.getExamItem().getId());
        return referenceRangeDTO;
    }

    private ReferenceRange mapToEntity(final ReferenceRangeDTO referenceRangeDTO,
            final ReferenceRange referenceRange) {
        referenceRange.setSex(referenceRangeDTO.getSex());
        referenceRange.setAgeMinYears(referenceRangeDTO.getAgeMinYears());
        referenceRange.setAgeMaxYears(referenceRangeDTO.getAgeMaxYears());
        referenceRange.setVersion(referenceRangeDTO.getVersion());
        referenceRange.setSource(referenceRangeDTO.getSource());
        referenceRange.setValidFrom(referenceRangeDTO.getValidFrom());
        referenceRange.setValidUntil(referenceRangeDTO.getValidUntil());
        referenceRange.setCreatedAt(referenceRangeDTO.getCreatedAt());
        final ExamItem examItem = referenceRangeDTO.getExamItem() == null ? null : examItemRepository.findById(referenceRangeDTO.getExamItem())
                .orElseThrow(() -> new NotFoundException("examItem not found"));
        referenceRange.setExamItem(examItem);
        return referenceRange;
    }

    @EventListener(BeforeDeleteExamItem.class)
    public void on(final BeforeDeleteExamItem event) {
        final ReferencedException referencedException = new ReferencedException();
        final ReferenceRange examItemReferenceRange = referenceRangeRepository.findFirstByExamItemId(event.getId());
        if (examItemReferenceRange != null) {
            referencedException.setKey("examItem.referenceRange.examItem.referenced");
            referencedException.addParam(examItemReferenceRange.getId());
            throw referencedException;
        }
    }

}
