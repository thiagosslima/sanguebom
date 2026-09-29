package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import br.com.fiap.sanguebom.model.catalog.ReferenceRangeCatalogDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.ExamItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExamItemCatalogResource.class)
class ExamItemCatalogResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamItemService examItemService;

    @Test
    @DisplayName("GET exam-items usa paginacao padrao e devolve o catalogo")
    void shouldListCatalogWithDefaultPagination() throws Exception {
        given(examItemService.findActiveCatalogItems(null, 0, 10))
                .willReturn(new PageResponse<>(List.of(item()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/v1/exam-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("GLI_JEJUM"))
                .andExpect(jsonPath("$.content[0].unit").value("mg/dL"))
                .andExpect(jsonPath("$.totalElements").value(1));

        then(examItemService).should().findActiveCatalogItems(null, 0, 10);
    }

    @Test
    @DisplayName("GET exam-items repassa categoria e paginacao informadas")
    void shouldForwardCategoryAndPagination() throws Exception {
        given(examItemService.findActiveCatalogItems("BIOQUIMICA", 1, 100))
                .willReturn(new PageResponse<>(List.of(), 1, 100, 0, 0));

        mockMvc.perform(get("/api/v1/exam-items")
                        .param("category", "BIOQUIMICA")
                        .param("page", "1")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(100));

        then(examItemService).should().findActiveCatalogItems("BIOQUIMICA", 1, 100);
    }

    @Test
    @DisplayName("GET exam-items rejeita size abaixo do minimo")
    void shouldRejectSizeBelowMinimum() throws Exception {
        mockMvc.perform(get("/api/v1/exam-items")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetros inválidos"))
                .andExpect(jsonPath("$.['parâmetros inválidos'][0].field").value("size"));

        then(examItemService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET exam-items rejeita page negativa")
    void shouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/exam-items")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['parâmetros inválidos'][0].field").value("page"));

        then(examItemService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET reference-ranges devolve as faixas vigentes do item")
    void shouldReturnReferenceRanges() throws Exception {
        given(examItemService.findCurrentReferenceRanges("GLI_JEJUM"))
                .willReturn(List.of(range()));

        mockMvc.perform(get("/api/v1/exam-items/{code}/reference-ranges", "GLI_JEJUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5L))
                .andExpect(jsonPath("$[0].source").value("SBPC/ML"))
                .andExpect(jsonPath("$[0].rules").isEmpty());

        then(examItemService).should().findCurrentReferenceRanges("GLI_JEJUM");
    }

    private static ExamItemCatalogDTO item() {
        return new ExamItemCatalogDTO(1L, "GLI_JEJUM", "Glicemia em Jejum", "mg/dL", "BIOQUIMICA", null);
    }

    private static ReferenceRangeCatalogDTO range() {
        return new ReferenceRangeCatalogDTO(
                5L, null, null, null, "v1", "SBPC/ML", LocalDate.of(2024, 1, 1), null, List.of());
    }
}
