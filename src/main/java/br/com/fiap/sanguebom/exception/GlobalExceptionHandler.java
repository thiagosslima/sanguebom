package br.com.fiap.sanguebom.exception;

import br.com.fiap.sanguebom.exception.dto.InvalidParamDto;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplicationException(ApplicationException e) {
        return e.toProblemDetail();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<InvalidParamDto> invalidParams = e.getFieldErrors()
                .stream().
                map(fe -> new InvalidParamDto(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatus(400);
        problemDetail.setTitle("Parâmetros inválidos");
        problemDetail.setDetail("Há campos inválidos na solicitação");
        problemDetail.setProperty("parâmetros inválidos", invalidParams);
        return problemDetail;

    }


}
