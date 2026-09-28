package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.model.doctor.ComparedExamDTO;
import br.com.fiap.sanguebom.model.doctor.ComparedExamItemDTO;
import br.com.fiap.sanguebom.model.doctor.ComparedExamValueDTO;
import br.com.fiap.sanguebom.model.doctor.ExamComparisonDTO;
import br.com.fiap.sanguebom.model.doctor.ExamItemVariationDTO;
import br.com.fiap.sanguebom.model.enums.ExamResultFlag;
import br.com.fiap.sanguebom.model.enums.ExamStatus;
import br.com.fiap.sanguebom.service.DoctorExamComparisonService;
import br.com.fiap.sanguebom.service.DoctorTimelineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorTimelineResource.class)
class DoctorTimelineResourceTest {

    private static final long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorTimelineService doctorTimelineService;

    @MockitoBean
    private DoctorExamComparisonService doctorExamComparisonService;

    @Test
    @DisplayName("GET compare converte examIds e devolve comparacao")
    void shouldCompareExams() throws Exception {
        given(doctorExamComparisonService.compare(USER_ID, List.of(10L, 20L)))
                .willReturn(comparison());

        mockMvc.perform(get("/api/v1/doctor/patients/{userId}/exams/compare", USER_ID)
                        .param("examIds", "10,20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.exams[0].examId").value(10L))
                .andExpect(jsonPath("$.items[0].itemCode").value("GLI_JEJUM"))
                .andExpect(jsonPath("$.items[0].values[1].valueNumeric").value(110))
                .andExpect(jsonPath("$.items[0].variations[0].absoluteVariation").value(22))
                .andExpect(jsonPath("$.items[0].variations[0].percentVariation").value(25.00));

        then(doctorExamComparisonService).should().compare(USER_ID, List.of(10L, 20L));
    }

    private static ExamComparisonDTO comparison() {
        return new ExamComparisonDTO(
                USER_ID,
                List.of(
                        exam(10L, 2024),
                        exam(20L, 2025)),
                List.of(new ComparedExamItemDTO(
                        "GLI_JEJUM",
                        "Glicemia em Jejum",
                        "mg/dL",
                        List.of(
                                value(10L, "88"),
                                value(20L, "110")),
                        List.of(new ExamItemVariationDTO(
                                10L,
                                20L,
                                new BigDecimal("22"),
                                new BigDecimal("25.00"))))));
    }

    private static ComparedExamDTO exam(final Long id, final int year) {
        return new ComparedExamDTO(
                id,
                OffsetDateTime.of(year, 8, 10, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(year, 8, 11, 10, 0, 0, 0, ZoneOffset.UTC),
                ExamStatus.RELEASED);
    }

    private static ComparedExamValueDTO value(final Long examId, final String value) {
        return new ComparedExamValueDTO(
                examId,
                new BigDecimal(value),
                null,
                "mg/dL",
                ExamResultFlag.NORMAL);
    }
}
