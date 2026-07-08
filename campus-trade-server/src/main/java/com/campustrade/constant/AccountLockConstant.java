package com.campustrade.constant;

public class AccountLockConstant {

    public static final String LOGIN_FAIL_PREFIX = "login:fail:";
    public static final int MAX_LOGIN_FAIL = 5;
    public static final long LOCK_DURATION = 1800;

    public static final String REGISTER_LIMIT_PREFIX = "register:limit:";
    public static final int MAX_REGISTER_PER_IP = 3;
    public static final long REGISTER_LIMIT_TTL = 3600;
}