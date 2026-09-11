package br.com.fiap.sanguebom.constants;

public enum NotificationStatus {
    PENDING ("pending"),
    SENT ("sent"),
    READ ("read"),
    UNREAD ("unread");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }

    public String getStatus() {
        return value;
    }
}
