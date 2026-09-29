package br.com.fiap.sanguebom.config;

import br.com.fiap.sanguebom.config.swagger.docs.SwaggerDocumentation;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwaggerConfigTest {

    private final OperationCustomizer customizer = new SwaggerConfig().operationCustomizer();

    @Test
    @DisplayName("mantem a operacao intacta quando o metodo nao tem @SwaggerDocumentation")
    void shouldKeepOperationWithoutAnnotation() throws Exception {
        final Operation operation = new Operation().description("original");

        final Operation result = customizer.customize(operation, handlerMethod("semDocumentacao"));

        assertThat(result).isSameAs(operation);
        assertThat(result.getDescription()).isEqualTo("original");
    }

    @Test
    @DisplayName("carrega a descricao do arquivo apontado por @SwaggerDocumentation")
    void shouldLoadDescriptionFromFile() throws Exception {
        final Operation operation = new Operation().description("original");

        final Operation result = customizer.customize(operation, handlerMethod("comDocumentacao"));

        assertThat(result).isSameAs(operation);
        assertThat(result.getDescription())
                .isEqualTo("## Documentação de teste\n\nConteúdo carregado do arquivo.\n");
    }

    @Test
    @DisplayName("falha quando o arquivo de documentacao nao existe")
    void shouldFailWhenFileIsMissing() throws Exception {
        final HandlerMethod handlerMethod = handlerMethod("documentacaoInexistente");

        assertThatThrownBy(() -> customizer.customize(new Operation(), handlerMethod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Documentação Swagger não encontrada: test/inexistente.md");
    }

    private static HandlerMethod handlerMethod(final String name) throws NoSuchMethodException {
        return new HandlerMethod(new FakeResource(), FakeResource.class.getMethod(name));
    }

    static class FakeResource {

        public void semDocumentacao() {
        }

        @SwaggerDocumentation("test/exemplo.md")
        public void comDocumentacao() {
        }

        @SwaggerDocumentation("test/inexistente.md")
        public void documentacaoInexistente() {
        }
    }
}
