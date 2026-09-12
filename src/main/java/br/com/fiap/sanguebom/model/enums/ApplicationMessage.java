package br.com.fiap.sanguebom.model.enums;

public enum ApplicationMessage {

    EXAM_MEDICAL_DISCLAIMER("exam.medical-disclaimer"),
    EXAM_ANALYSIS_MISSING_BIRTH_DATE("exam.analysis.missing-birth-date"),
    EXAM_ANALYSIS_MISSING_HEALTH_PROFILE("exam.analysis.missing-health-profile"),
    EXAM_ANALYSIS_MISSING_SEX("exam.analysis.missing-sex"),
    EXAM_ANALYSIS_RULE_NOT_FOUND("exam.analysis.rule-not-found"),
    EXAM_ANALYSIS_EMPTY_ITEMS("exam.analysis.empty-items"),
    DOCTOR_TIMELINE_PATIENT_NOT_FOUND("doctor.timeline.patient-not-found"),
    DOCTOR_TIMELINE_EXAM_ITEM_NOT_FOUND("doctor.timeline.exam-item-not-found"),
    DOCTOR_TIMELINE_INVALID_PERIOD("doctor.timeline.invalid-period"),
    DOCTOR_EXAM_COMPARISON_PATIENT_NOT_FOUND("doctor.exam-comparison.patient-not-found"),
    DOCTOR_EXAM_COMPARISON_INSUFFICIENT_EXAMS("doctor.exam-comparison.insufficient-exams"),
    DOCTOR_EXAM_COMPARISON_EXAM_NOT_FOUND("doctor.exam-comparison.exam-not-found"),
    DOCTOR_EXAM_COMPARISON_DUPLICATED_EXAMS("doctor.exam-comparison.duplicated-exams"),
    RISK_ASSESSMENT_LOW_EXPLANATION("risk-assessment.level.low.explanation"),
    RISK_ASSESSMENT_MODERATE_EXPLANATION("risk-assessment.level.moderate.explanation"),
    RISK_ASSESSMENT_HIGH_EXPLANATION("risk-assessment.level.high.explanation"),
    RISK_ASSESSMENT_VERY_HIGH_EXPLANATION("risk-assessment.level.very-high.explanation");

    private final String code;

    ApplicationMessage(String code) {
        this.code = code;
    }

    public static ApplicationMessage from(RiskAssessmentLevel level) {
        return switch (level) {
            case LOW, NORMAL -> RISK_ASSESSMENT_LOW_EXPLANATION;
            case MODERATE, ALERTA -> RISK_ASSESSMENT_MODERATE_EXPLANATION;
            case HIGH -> RISK_ASSESSMENT_HIGH_EXPLANATION;
            case VERY_HIGH -> RISK_ASSESSMENT_VERY_HIGH_EXPLANATION;
        };
    }

    public String getCode() {
        return code;
    }
}
