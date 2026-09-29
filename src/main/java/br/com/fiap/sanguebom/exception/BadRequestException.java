package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class BadRequestException extends ApplicationException {
    public BadRequestException(String message) {
        super(message);
    }
    public BadRequestException(Throwable cause) {
        super(cause);
    }

    @Override
    public ProblemDetail toProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(400);
        problemDetail.setTitle("Bad Request");
        problemDetail.setDetail(getMessage());
        return problemDetail;
    }
}
