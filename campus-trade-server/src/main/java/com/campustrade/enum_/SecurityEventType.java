package com.campustrade.enum_;

public enum SecurityEventType {
    LOGIN_FAIL("LOGIN_FAIL"),
    LOGIN_SUCCESS("LOGIN_SUCCESS"),
    ACCESS_DENIED("ACCESS_DENIED"),
    TOKEN_EXPIRED("TOKEN_EXPIRED"),
    RATE_LIMIT("RATE_LIMIT"),
    MALICIOUS_INPUT("MALICIOUS_INPUT"),
    PASSWORD_RESET("PASSWORD_RESET");

    private final String code;

    SecurityEventType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
