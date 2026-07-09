package com.campustrade.enum_;

public enum OrderStatus {
    PENDING_PAY("PENDING_PAY"),
    PAID("PAID"),
    SHIPPING("SHIPPING"),
    PENDING_REVIEW("PENDING_REVIEW"),
    FINISHED("FINISHED"),
    CANCELLED("CANCELLED"),
    REFUND("REFUND");

    private final String code;

    OrderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
