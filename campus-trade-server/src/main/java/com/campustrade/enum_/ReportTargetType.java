package com.campustrade.enum_;

public enum ReportTargetType {
    GOODS(1),
    USER(2),
    CHAT(3);

    private final int code;

    ReportTargetType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}