package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.enums.RiskAssessmentLevel;
import br.com.fiap.sanguebom.model.riskAssessment.RiskTimelinePointDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.RiskAssessmentService;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.servlet.ServletErrorHandlingConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskAssessmentResource.class)
@ImportAutoConfiguration(ServletErrorHandlingConfiguration.class)
class RiskAssessmentResourceTest {

    private static final long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskAssessmentService riskAssessmentService;

    @Test
    @DisplayName("GET /api/riskAssessments devolve a lista de todos os riscos")
    void shouldReturnAllRiskAssessments() throws Exception {
        mockMvc.perform(get("/api/riskAssessments"))
                .andExpect(status().isOk());

        then(riskAssessmentService).should().findAll();
    }

    @Test
    void shouldReturnRiskAssessmentById() throws Exception {
        var riskAssessmentId = 1L;

        mockMvc.perform(get("/api/riskAssessments/{id}", riskAssessmentId))
                .andExpect(status().isOk());

        then(riskAssessmentService).should().get(riskAssessmentId);
    }


    @Test
    @DisplayName("GET /api/riskAssessments/{userId}/timeline devolve a página com a série histórica")
    void shouldReturnTimeline() throws Exception {
        var from = LocalDate.of(2026, 8, 1);
        var to = LocalDate.of(2026, 8, 31);

        given(riskAssessmentService.findByUserId(USER_ID, from, to, 0, 10))
                .willReturn(new PageResponse<>(
                        List.of(
                                new RiskTimelinePointDTO(
                                        10L,
                                        20L,
                                        new BigDecimal("34.08"),
                                        RiskAssessmentLevel.MODERATE,
                                        OffsetDateTime.of(2026, 8, 10, 8, 30, 0, 0, ZoneOffset.UTC)
                                ),
                                new RiskTimelinePointDTO(
                                        11L,
                                        21L,
                                        new BigDecimal("58.20"),
                                        RiskAssessmentLevel.HIGH,
                                        OffsetDateTime.of(2026, 8, 11, 9, 0, 0, 0, ZoneOffset.UTC)
                                )
                        ),
                        0,
                        10,
                        2L,
                        1
                ));

        mockMvc.perform(get("/api/riskAssessments/{userId}/timeline", USER_ID)
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].examId").value(20))
                .andExpect(jsonPath("$.content[0].score").value(34.08))
                .andExpect(jsonPath("$.content[0].level").value("MODERATE"))
                .andExpect(jsonPath("$.content[1].level").value("HIGH"));

        then(riskAssessmentService).should()
                .findByUserId(USER_ID, from, to, 0, 10);
    }

    @Test
    @DisplayName("GET /api/riskAssessments/{userId}/timeline com page/size inválidos devolve 400")
    void shouldRejectInvalidPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/riskAssessments/{userId}/timeline", USER_ID)
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        then(riskAssessmentService).shouldHaveNoInteractions();
    }

    @Test
    void shouldUpdateRiskAssessment() throws Exception {
        var riskAssessmentId = 1L;

        mockMvc.perform(put("/api/riskAssessments/{id}", riskAssessmentId)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("{\"score\": 75.5, \"level\": \"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(riskAssessmentId));

        then(riskAssessmentService).should().update(
                eq(riskAssessmentId),
                argThat(dto ->
                        dto.getScore().compareTo(new BigDecimal("75.5")) == 0
                                && dto.getLevel() == RiskAssessmentLevel.HIGH
                )
        );
    }
}