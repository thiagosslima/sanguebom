package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class DuplicatedAchievementException extends ApplicationException {

    private final String detail;

    public DuplicatedAchievementException(String detail) {
        super(detail);
        this.detail = detail;
    }

    @Override
    public ProblemDetail toProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(422);
        problemDetail.setTitle("Conquista já cadastrada");
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
