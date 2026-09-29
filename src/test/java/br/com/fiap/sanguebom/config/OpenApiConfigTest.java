package br.com.fiap.sanguebom.config;

import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("define titulo, versao e descricao da API")
    void shouldDescribeApi() {
        final Info info = new OpenApiConfig().customOpenAPI().getInfo();

        assertThat(info.getTitle()).isEqualTo("Sangue Bom API");
        assertThat(info.getVersion()).isEqualTo("1.0.0");
        assertThat(info.getDescription()).isEqualTo("API para gerenciamento de exames de sangue");
    }
}
