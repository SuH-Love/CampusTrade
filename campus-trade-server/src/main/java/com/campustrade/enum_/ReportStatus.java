package com.campustrade.enum_;

public enum ReportStatus {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    FINISHED("FINISHED"),
    RESOLVED("RESOLVED"),
    DISMISSED("DISMISSED");

    private final String code;

    ReportStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
