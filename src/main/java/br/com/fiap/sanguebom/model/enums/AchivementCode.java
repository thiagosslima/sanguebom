package br.com.fiap.sanguebom.model.enums;

public enum AchivementCode {

    SANGUE_BOM,
    HEALTH_CHAMPION,
    PUNCTUAL;

    public String getCode() {
        return name();
    }
}
