package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
    public BadRequestException(Throwable cause) {
        super(cause);
    }

    ProblemDetail toProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(400);
        problemDetail.setTitle("Bad Request");
        problemDetail.setDetail(getMessage());
        return problemDetail;
    }
}
