package com.campustrade.service;

public interface EmailService {
    boolean isConfigured();
    void sendVerificationCode(String toEmail, String code);
}