package com.campustrade.util;

import java.util.regex.Pattern;

public class PasswordUtil {

    private static final Pattern UPPER_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        int typeCount = 0;
        if (UPPER_PATTERN.matcher(password).find()) typeCount++;
        if (LOWER_PATTERN.matcher(password).find()) typeCount++;
        if (DIGIT_PATTERN.matcher(password).find()) typeCount++;
        if (SPECIAL_PATTERN.matcher(password).find()) typeCount++;
        return typeCount >= 3;
    }

    public static String getPasswordRequirement() {
        return "密码长度至少8位，必须包含大写字母、小写字母、数字、特殊字符中的至少3种";
    }
}