package br.com.fiap.sanguebom.exception;

import org.springframework.http.ProblemDetail;

public class ExamOwnerChangeException extends ApplicationException {

    public ExamOwnerChangeException() {
        super("Não é permitido alterar o cidadão de um exame cadastrado.");
    }

    @Override
    public ProblemDetail toProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setTitle("Alteração de cidadão não permitida");
        problem.setDetail(getMessage());
        return problem;
    }
}
