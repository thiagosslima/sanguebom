package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class DuplicatedExamResultException extends ApplicationException {

    private final String detail;

    public DuplicatedExamResultException(String detail) {
        super(detail);
        this.detail = detail;
    }

    @Override
    public ProblemDetail toProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(422);
        problemDetail.setTitle("Itens de exame duplicados");
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
