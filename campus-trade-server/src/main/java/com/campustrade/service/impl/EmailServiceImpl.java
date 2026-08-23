package com.campustrade.service.impl;

import com.campustrade.service.EmailService;
import com.campustrade.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.mail.Session;
import java.util.Properties;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private SystemConfigService systemConfigService;

    @Override
    public boolean isConfigured() {
        String username = systemConfigService.getDecryptedValue("mail.username");
        String password = systemConfigService.getDecryptedValue("mail.password");
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        String host = systemConfigService.getDecryptedValue("mail.host");
        String portStr = systemConfigService.getDecryptedValue("mail.port");
        String username = systemConfigService.getDecryptedValue("mail.username");
        String password = systemConfigService.getDecryptedValue("mail.password");
        String fromName = systemConfigService.getDecryptedValue("mail.from");
        String sslStr = systemConfigService.getDecryptedValue("mail.ssl");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            log.warn("Mail not configured, cannot send verification code to {}", toEmail);
            throw new RuntimeException("邮件服务未配置，请联系管理员");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host != null ? host : "smtp.qq.com");
        mailSender.setPort(portStr != null ? Integer.parseInt(portStr) : 465);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = new Properties();
        boolean ssl = sslStr == null || Boolean.parseBoolean(sslStr);
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", portStr != null ? portStr : "465");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        mailSender.setJavaMailProperties(props);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(username);
        message.setTo(toEmail);
        message.setSubject("【CampusTrade】重置密码验证码");
        message.setText("您正在重置密码，验证码为：" + code + "\n\n验证码有效期为5分钟，请尽快使用。\n如非本人操作，请忽略此邮件。\n\n—— CampusTrade校园贸易平台");

        try {
            mailSender.send(message);
            log.info("Verification code email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification code email to {}", toEmail, e);
            throw new RuntimeException("邮件发送失败：" + e.getMessage(), e);
        }
    }
}