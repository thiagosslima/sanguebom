package br.com.fiap.sanguebom.service;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.ExamResultDTO;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.ExamResult;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.repository.ExamItemRepository;
import br.com.fiap.sanguebom.repository.ExamRepository;
import br.com.fiap.sanguebom.repository.ExamResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceTest {

    private static final long EXAM_ID = 10L;
    private static final long ITEM_ID = 20L;

    @Mock
    private ExamResultRepository examResultRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamItemRepository examItemRepository;

    @InjectMocks
    private ExamResultService examResultService;

    private Exam exam() {
        Exam e = new Exam();
        e.setId(EXAM_ID);
        return e;
    }

    private ExamItem item() {
        ExamItem i = new ExamItem();
        i.setId(ITEM_ID);
        return i;
    }

    private ExamResult entity() {
        ExamResult r = new ExamResult();
        r.setId(1L);
        r.setValueNumeric(new BigDecimal("13.5000"));
        r.setValueText("13,5");
        r.setUnit("g/dL");
        r.setFlag(ExamResultFlag.NORMAL);
        r.setExam(exam());
        r.setExamItem(item());
        return r;
    }

    private ExamResultDTO dto(Long examId, Long itemId) {
        ExamResultDTO d = new ExamResultDTO();
        d.setValueNumeric(new BigDecimal("7.2000"));
        d.setValueText("7,2");
        d.setUnit("mg/dL");
        d.setFlag(ExamResultFlag.ATTENTION);
        d.setExam(examId);
        d.setExamItem(itemId);
        return d;
    }

    @Test
    @DisplayName("findAll achata exame e item de exame para os respectivos ids")
    void findAllMapsEveryField() {
        when(examResultRepository.findAll(Sort.by("id"))).thenReturn(List.of(entity()));

        assertThat(examResultService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getValueNumeric()).isEqualByComparingTo("13.5000");
            assertThat(d.getValueText()).isEqualTo("13,5");
            assertThat(d.getUnit()).isEqualTo("g/dL");
            assertThat(d.getFlag()).isEqualTo(ExamResultFlag.NORMAL);
            assertThat(d.getExam()).isEqualTo(EXAM_ID);
            assertThat(d.getExamItem()).isEqualTo(ITEM_ID);
        });
    }

    @Test
    @DisplayName("findAll tolera resultado sem exame e sem item associados")
    void findAllToleratesNullRelations() {
        ExamResult orphan = entity();
        orphan.setExam(null);
        orphan.setExamItem(null);
        when(examResultRepository.findAll(Sort.by("id"))).thenReturn(List.of(orphan));

        assertThat(examResultService.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getExam()).isNull();
            assertThat(d.getExamItem()).isNull();
        });
    }

    @Test
    @DisplayName("get devolve o resultado existente e lança NotFoundException quando não existe")
    void getBehaviour() {
        when(examResultRepository.findById(1L)).thenReturn(Optional.of(entity()));
        assertThat(examResultService.get(1L).getUnit()).isEqualTo("g/dL");

        when(examResultRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> examResultService.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create resolve exame e item de exame antes de salvar")
    void createResolvesBothRelations() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
        when(examItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item()));
        ExamResult saved = entity();
        saved.setId(44L);
        when(examResultRepository.save(any(ExamResult.class))).thenReturn(saved);

        Long id = examResultService.create(dto(EXAM_ID, ITEM_ID));

        ArgumentCaptor<ExamResult> captor = ArgumentCaptor.forClass(ExamResult.class);
        verify(examResultRepository).save(captor.capture());

        assertThat(id).isEqualTo(44L);
        assertThat(captor.getValue().getExam().getId()).isEqualTo(EXAM_ID);
        assertThat(captor.getValue().getExamItem().getId()).isEqualTo(ITEM_ID);
        assertThat(captor.getValue().getFlag()).isEqualTo(ExamResultFlag.ATTENTION);
    }

    @Test
    @DisplayName("create falha quando o exame informado não existe")
    void createFailsWhenExamMissing() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examResultService.create(dto(EXAM_ID, ITEM_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("exam");

        verify(examResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("create falha quando o item de exame informado não existe")
    void createFailsWhenExamItemMissing() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
        when(examItemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examResultService.create(dto(EXAM_ID, ITEM_ID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("examItem");

        verify(examResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("update aplica o DTO sobre o resultado existente")
    void updateMutatesExisting() {
        ExamResult existing = entity();
        when(examResultRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
        when(examItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item()));

        examResultService.update(1L, dto(EXAM_ID, ITEM_ID));

        verify(examResultRepository).save(existing);
        assertThat(existing.getUnit()).isEqualTo("mg/dL");
        assertThat(existing.getFlag()).isEqualTo(ExamResultFlag.ATTENTION);
    }

    @Test
    @DisplayName("update lança NotFoundException e não salva quando o id não existe")
    void updateThrowsWhenMissing() {
        when(examResultRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examResultService.update(9L, dto(EXAM_ID, ITEM_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(examResultRepository, never()).save(any());
    }
}
