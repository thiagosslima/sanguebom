package br.com.fiap.sanguebom.exception;

import br.com.fiap.sanguebom.exception.dto.InvalidParamDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_PARAMS_TITLE = "Parâmetros inválidos";
    private static final String INVALID_PARAMS_DETAIL = "Há campos inválidos na solicitação";
    private static final String INVALID_PARAMS_PROPERTY = "parâmetros inválidos";

    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplicationException(ApplicationException e) {
        return e.toProblemDetail();
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Recurso não encontrado");
        problemDetail.setDetail(e.getMessage() == null ? "Recurso não encontrado" : e.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<InvalidParamDto> invalidParams = e.getFieldErrors()
                .stream().
                map(fe -> new InvalidParamDto(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return invalidParams(invalidParams);
    }

    /**
     * Violacoes em parametros de rota e de query, validados via {@code @Validated} no controller.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException e) {
        List<InvalidParamDto> invalidParams = e.getConstraintViolations()
                .stream()
                .map(violation -> new InvalidParamDto(fieldOf(violation), violation.getMessage()))
                .toList();

        return invalidParams(invalidParams);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return invalidParams(List.of(new InvalidParamDto(e.getName(), "Valor inválido para o parâmetro")));
    }

    private static ProblemDetail invalidParams(final List<InvalidParamDto> invalidParams) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle(INVALID_PARAMS_TITLE);
        problemDetail.setDetail(INVALID_PARAMS_DETAIL);
        problemDetail.setProperty(INVALID_PARAMS_PROPERTY, invalidParams);
        return problemDetail;
    }

    /** O caminho vem como "getExams.size"; interessa so o nome do parametro. */
    private static String fieldOf(final ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot < 0 ? path : path.substring(lastDot + 1);
    }

}
