package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(Throwable cause) {
        super(cause);
    }

    public ProblemDetail toProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(500);
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setDetail("Contact Support");
        return problemDetail;
    }
}
