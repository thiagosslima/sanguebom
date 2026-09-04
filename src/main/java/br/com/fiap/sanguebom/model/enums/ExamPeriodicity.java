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

    public static ExamPeriodicity from(final String value) {
        if (value == null) {
            return YEARLY;
        }
        for (final ExamPeriodicity periodicity : values()) {
            if (periodicity.name().equalsIgnoreCase(value.trim())) {
                return periodicity;
            }
        }
        return YEARLY;
    }
}
