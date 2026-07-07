package com.campustrade.enum_;

public enum NotificationType {
    SYSTEM("SYSTEM"),
    ORDER("ORDER"),
    GOODS("GOODS"),
    REPORT("REPORT"),
    CHAT("CHAT");

    private final String code;

    NotificationType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
