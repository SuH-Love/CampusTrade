package com.campustrade.enum_;

public enum GoodsStatus {
    DRAFT("DRAFT"),
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    SOLD("SOLD");

    private final String code;

    GoodsStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
