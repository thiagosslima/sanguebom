package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.exception.NotFoundException;
import br.com.fiap.sanguebom.model.dtos.AchievementDTO;
import br.com.fiap.sanguebom.model.dtos.AppUserDTO;
import br.com.fiap.sanguebom.model.dtos.ExamItemDTO;
import br.com.fiap.sanguebom.model.dtos.HealthUnitDTO;
import br.com.fiap.sanguebom.model.enums.AchivementCode;
import br.com.fiap.sanguebom.service.AchievementService;
import br.com.fiap.sanguebom.service.AppUserService;
import br.com.fiap.sanguebom.service.ExamItemService;
import br.com.fiap.sanguebom.service.ExamResultService;
import br.com.fiap.sanguebom.service.ExamService;
import br.com.fiap.sanguebom.service.HealthProfileService;
import br.com.fiap.sanguebom.service.HealthUnitService;
import br.com.fiap.sanguebom.service.ReferenceRangeService;
import br.com.fiap.sanguebom.service.RuleService;
import br.com.fiap.sanguebom.service.UserAchievementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os dez Resources de CRUD são estruturalmente idênticos e finos, mas
 * carregam contrato de verdade: rota, verbo, código de status (201 no POST,
 * 200 no PUT) e serialização do corpo. Um único contexto web cobre todos,
 * em vez de dez classes de teste copiadas.
 */
@WebMvcTest(controllers = {
        AchievementResource.class,
        AppUserResource.class,
        ExamItemResource.class,
        ExamResource.class,
        ExamResultResource.class,
        HealthProfileResource.class,
        HealthUnitResource.class,
        ReferenceRangeResource.class,
        RuleResource.class,
        UserAchievementResource.class
})
class CrudResourcesTest {

    @Autowired
    private MockMvc mockMvc;

    // O slice @WebMvcTest não expõe um bean de ObjectMapper; aqui ele serve
    // só para montar o corpo das requisições, então uma instância própria basta.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private AchievementService achievementService;
    @MockitoBean private AppUserService appUserService;
    @MockitoBean private ExamItemService examItemService;
    @MockitoBean private ExamService examService;
    @MockitoBean private ExamResultService examResultService;
    @MockitoBean private HealthProfileService healthProfileService;
    @MockitoBean private HealthUnitService healthUnitService;
    @MockitoBean private ReferenceRangeService referenceRangeService;
    @MockitoBean private RuleService ruleService;
    @MockitoBean private UserAchievementService userAchievementService;

    @Nested
    @DisplayName("/api/achievements")
    class Achievements {

        private AchievementDTO dto() {
            AchievementDTO d = new AchievementDTO();
            d.setId(1L);
            d.setCode(AchivementCode.SANGUE_BOM);
            d.setName("Sangue Bom");
            d.setActive(true);
            return d;
        }

        @Test
        @DisplayName("GET lista devolve 200 e serializa o enum do código")
        void listReturnsOk() throws Exception {
            when(achievementService.findAll()).thenReturn(List.of(dto()));

            mockMvc.perform(get("/api/achievements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].code").value("SANGUE_BOM"));
        }

        @Test
        @DisplayName("GET por id devolve 200 com o recurso")
        void getByIdReturnsOk() throws Exception {
            when(achievementService.get(1L)).thenReturn(dto());

            mockMvc.perform(get("/api/achievements/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Sangue Bom"));
        }

        @Test
        @DisplayName("GET por id inexistente devolve 404, e não 500")
        void getByIdReturnsNotFound() throws Exception {
            when(achievementService.get(404L)).thenThrow(new NotFoundException("não encontrado"));

            mockMvc.perform(get("/api/achievements/{id}", 404L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST devolve 201 e o id criado no corpo")
        void createReturnsCreated() throws Exception {
            when(achievementService.create(any(AchievementDTO.class))).thenReturn(77L);

            mockMvc.perform(post("/api/achievements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").value(77));
        }

        @Test
        @DisplayName("PUT devolve 200 e delega ao service com o id da rota")
        void updateReturnsOk() throws Exception {
            mockMvc.perform(put("/api/achievements/{id}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(5));

            verify(achievementService).update(eq(5L), any(AchievementDTO.class));
        }

        @Test
        @DisplayName("PUT em id inexistente propaga 404")
        void updateReturnsNotFound() throws Exception {
            doThrow(new NotFoundException("não encontrado"))
                    .when(achievementService).update(eq(404L), any(AchievementDTO.class));

            mockMvc.perform(put("/api/achievements/{id}", 404L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("/api/appUsers")
    class AppUsers {

        private AppUserDTO dto() {
            AppUserDTO d = new AppUserDTO();
            d.setId(1L);
            d.setName("Maria");
            d.setEmail("maria@example.com");
            d.setStatus("ACTIVE");
            return d;
        }

        @Test
        void listReturnsOk() throws Exception {
            when(appUserService.findAll()).thenReturn(List.of(dto()));

            mockMvc.perform(get("/api/appUsers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("maria@example.com"));
        }

        @Test
        void getByIdReturnsOk() throws Exception {
            when(appUserService.get(1L)).thenReturn(dto());

            mockMvc.perform(get("/api/appUsers/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Maria"));
        }

        @Test
        void createReturnsCreated() throws Exception {
            when(appUserService.create(any(AppUserDTO.class))).thenReturn(9L);

            mockMvc.perform(post("/api/appUsers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").value(9));
        }

        @Test
        void updateReturnsOk() throws Exception {
            mockMvc.perform(put("/api/appUsers/{id}", 3L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isOk());

            verify(appUserService).update(eq(3L), any(AppUserDTO.class));
        }
    }

    @Nested
    @DisplayName("/api/healthUnits")
    class HealthUnits {

        private HealthUnitDTO dto() {
            HealthUnitDTO d = new HealthUnitDTO();
            d.setId(1L);
            d.setName("UBS Central");
            d.setCnes("1234567");
            return d;
        }

        @Test
        void listReturnsOk() throws Exception {
            when(healthUnitService.findAll()).thenReturn(List.of(dto()));

            mockMvc.perform(get("/api/healthUnits"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].cnes").value("1234567"));
        }

        @Test
        void getByIdReturnsOk() throws Exception {
            when(healthUnitService.get(1L)).thenReturn(dto());

            mockMvc.perform(get("/api/healthUnits/{id}", 1L))
                    .andExpect(status().isOk());
        }

        @Test
        void createReturnsCreated() throws Exception {
            when(healthUnitService.create(any(HealthUnitDTO.class))).thenReturn(4L);

            mockMvc.perform(post("/api/healthUnits")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isCreated());
        }

        @Test
        void updateReturnsOk() throws Exception {
            mockMvc.perform(put("/api/healthUnits/{id}", 2L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isOk());

            verify(healthUnitService).update(eq(2L), any(HealthUnitDTO.class));
        }
    }

    @Nested
    @DisplayName("/api/examItems")
    class ExamItems {

        private ExamItemDTO dto() {
            ExamItemDTO d = new ExamItemDTO();
            d.setId(1L);
            d.setCode("HB");
            d.setName("Hemoglobina");
            return d;
        }

        @Test
        void listReturnsOk() throws Exception {
            when(examItemService.findAll()).thenReturn(List.of(dto()));

            mockMvc.perform(get("/api/examItems"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].code").value("HB"));
        }

        @Test
        void getByIdReturnsOk() throws Exception {
            when(examItemService.get(1L)).thenReturn(dto());

            mockMvc.perform(get("/api/examItems/{id}", 1L))
                    .andExpect(status().isOk());
        }

        @Test
        void createReturnsCreated() throws Exception {
            when(examItemService.create(any(ExamItemDTO.class))).thenReturn(6L);

            mockMvc.perform(post("/api/examItems")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isCreated());
        }

        @Test
        void updateReturnsOk() throws Exception {
            mockMvc.perform(put("/api/examItems/{id}", 8L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto())))
                    .andExpect(status().isOk());

            verify(examItemService).update(eq(8L), any(ExamItemDTO.class));
        }
    }
}
