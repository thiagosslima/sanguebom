package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class UserAlreadyExistsException extends ApplicationException {

    private final String detail;

    public UserAlreadyExistsException(String detail) {
        super(detail);
        this.detail = detail;
    }


    @Override
    public ProblemDetail toProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(422);
        problemDetail.setTitle("Usuário já cadastrado");
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
