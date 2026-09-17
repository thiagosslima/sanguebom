package br.com.fiap.sanguebom.model.enums;

public enum ExamPeriodicity {

    QUARTERLY(3),
    SEMESTERLY(6),
    YEARLY(12);

    private final int months;

    ExamPeriodicity(final int months) {
        this.months = months;
    }

    public int months() {
        return months;
    }
}
