package br.com.fiap.sanguebom.exception;

import br.com.fiap.sanguebom.controller.AchievementResource;
import br.com.fiap.sanguebom.exception.dto.InvalidParamDto;
import br.com.fiap.sanguebom.model.dtos.AchievementDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O handler é o que transforma exceções em respostas RFC 7807. Os testes
 * cobrem o status, o título e a lista de parâmetros inválidos que o cliente
 * usa para destacar campos no formulário.
 */
class GlobalExceptionHandlerTest {

    private static final String INVALID_PARAMS_PROPERTY = "parâmetros inválidos";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unchecked")
    private List<InvalidParamDto> invalidParamsOf(ProblemDetail detail) {
        assertThat(detail.getProperties()).isNotNull();
        return (List<InvalidParamDto>) detail.getProperties().get(INVALID_PARAMS_PROPERTY);
    }

    @Test
    @DisplayName("NotFoundException vira 404 com a mensagem original")
    void notFoundUsesOriginalMessage() {
        ProblemDetail detail = handler.handleNotFoundException(new NotFoundException("Exame 42 não existe"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getTitle()).isEqualTo("Recurso não encontrado");
        assertThat(detail.getDetail()).isEqualTo("Exame 42 não existe");
    }

    @Test
    @DisplayName("NotFoundException sem mensagem recebe um texto padrão, e não null")
    void notFoundFallsBackToDefaultMessage() {
        ProblemDetail detail = handler.handleNotFoundException(new NotFoundException());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getDetail()).isEqualTo("Recurso não encontrado");
    }

    @Test
    @DisplayName("ApplicationException delega a montagem do ProblemDetail à própria exceção")
    void applicationExceptionDelegatesToSubclass() {
        ProblemDetail detail = handler.handleApplicationException(
                new BadRequestException("Requisição inválida"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("Bad Request");
        assertThat(detail.getDetail()).isEqualTo("Requisição inválida");
    }

    @Test
    @DisplayName("ApplicationException genérica não vaza detalhe interno para o cliente")
    void genericApplicationExceptionHidesInternals() {
        ProblemDetail detail = handler.handleApplicationException(
                new ApplicationException("stack trace secreta com dados sensíveis"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getDetail()).isEqualTo("Contact Support");
        assertThat(detail.getDetail()).doesNotContain("secreta");
    }

    @Test
    @DisplayName("Erros de validação de corpo viram 400 listando campo e mensagem")
    void bodyValidationListsFieldErrors() throws Exception {
        var binding = new BeanPropertyBindingResult(new Object(), "achievementDTO");
        binding.addError(new FieldError("achievementDTO", "name", "não deve estar em branco"));
        binding.addError(new FieldError("achievementDTO", "code", "não deve ser nulo"));

        // Um método real de controller, em vez de um dummy: é exatamente o
        // tipo de parâmetro que o Spring reporta quando @Valid falha.
        MethodParameter parameter = new MethodParameter(
                AchievementResource.class.getDeclaredMethod("createAchievement", AchievementDTO.class), 0);

        ProblemDetail detail = handler.handleMethodArgumentNotValidException(
                new MethodArgumentNotValidException(parameter, binding));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("Parâmetros inválidos");
        assertThat(detail.getDetail()).isEqualTo("Há campos inválidos na solicitação");
        assertThat(invalidParamsOf(detail))
                .extracting(InvalidParamDto::field)
                .containsExactly("name", "code");
    }

    @Test
    @DisplayName("Violações em parâmetros de rota mostram só o nome do parâmetro, sem o prefixo do método")
    void constraintViolationStripsMethodPrefix() {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("getExams.size");

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("deve ser menor ou igual a 100");

        ProblemDetail detail = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of(violation)));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(invalidParamsOf(detail)).singleElement().satisfies(p -> {
            assertThat(p.field()).isEqualTo("size");
            assertThat(p.reason()).isEqualTo("deve ser menor ou igual a 100");
        });
    }

    @Test
    @DisplayName("Caminho sem ponto é usado inteiro como nome do parâmetro")
    void constraintViolationKeepsPathWithoutDot() {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("size");

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("inválido");

        ProblemDetail detail = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of(violation)));

        assertThat(invalidParamsOf(detail)).singleElement()
                .extracting(InvalidParamDto::field).isEqualTo("size");
    }

    @Test
    @DisplayName("Tipo errado no parâmetro vira 400 apontando o parâmetro problemático")
    void typeMismatchPointsAtTheParameter() {
        var exception = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, new IllegalArgumentException("nope"));

        ProblemDetail detail = handler.handleMethodArgumentTypeMismatchException(exception);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(invalidParamsOf(detail)).singleElement().satisfies(p -> {
            assertThat(p.field()).isEqualTo("id");
            assertThat(p.reason()).isEqualTo("Valor inválido para o parâmetro");
        });
    }
}
