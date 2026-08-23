package com.campustrade.constant;

public class SecurityConstant {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    public static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/captcha",
            "/api/auth/reset-password",
            "/api/auth/send-code",
            "/api/banner/active",
            "/api/rating/average/**",
            "/api/goods/hot-keywords",
            "/api/goods/suggest",
            "/api/user/*",
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/v2/api-docs/**",
            "/favicon.ico"
    };

    public static final int LOGIN_RATE_LIMIT = 5;
    public static final int LOGIN_RATE_PERIOD = 60;
}