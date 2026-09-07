package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class ExamAnalysisException extends ApplicationException {

    private final String detail;

    public ExamAnalysisException(String detail) {
        super(detail);
        this.detail = detail;
    }

    @Override
    public ProblemDetail toProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(422);
        problemDetail.setTitle("Análise de exame inválida");
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
