package br.com.fiap.sanguebom.config;

import br.com.fiap.sanguebom.controller.doc.SwaggerDocumentation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SwaggerConfig {

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            SwaggerDocumentation documentation = handlerMethod.getMethodAnnotation(SwaggerDocumentation.class);

            if (documentation == null) {
                return operation;
            }
            String description =
                    loadDocumentation(documentation.value());
            operation.setDescription(description);
            return operation;
        };
    }

    private String loadDocumentation(String path) {
        try (InputStream inputStream = getClass().getResourceAsStream("/swagger/docs/" + path)) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Documentação Swagger não encontrada: " + path
                );
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao carregar resource: " + path, e);
        }
    }
}
