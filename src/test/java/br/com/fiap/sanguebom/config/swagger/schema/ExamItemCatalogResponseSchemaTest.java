package br.com.fiap.sanguebom.config.swagger.schema;

import br.com.fiap.sanguebom.model.catalog.ExamItemCatalogDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExamItemCatalogResponseSchemaTest {

    @Test
    @DisplayName("expoe os campos da pagina do catalogo documentada no Swagger")
    void shouldExposePageFields() {
        final ExamItemCatalogDTO item =
                new ExamItemCatalogDTO(1L, "GLI_JEJUM", "Glicemia em Jejum", "mg/dL", "BIOQUIMICA", null);

        final ExamItemCatalogResponseSchema schema =
                new ExamItemCatalogResponseSchema(List.of(item), 0, 10, 1L, 1, true, true, false);

        assertThat(schema.content()).containsExactly(item);
        assertThat(schema.page()).isZero();
        assertThat(schema.size()).isEqualTo(10);
        assertThat(schema.totalElements()).isEqualTo(1L);
        assertThat(schema.totalPages()).isEqualTo(1);
        assertThat(schema.first()).isTrue();
        assertThat(schema.last()).isTrue();
        assertThat(schema.empty()).isFalse();
    }
}
