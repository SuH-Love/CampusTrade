package com.campustrade.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.campustrade.service.SystemConfigService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String gateway;
    private String signType;
    private String notifyUrl;
    private String returnUrl;

    @Autowired
    private SystemConfigService systemConfigService;

    private String resolve(String key, String ymlValue) {
        if (systemConfigService == null) return ymlValue;
        try {
            if ("alipay.private_key".equals(key) || "alipay.alipay_public_key".equals(key)) {
                String dbValue = systemConfigService.getDecryptedValue(key);
                return (dbValue != null && !dbValue.isEmpty()) ? dbValue : ymlValue;
            }
            String dbValue = systemConfigService.getConfigValue(key);
            return (dbValue != null && !dbValue.isEmpty()) ? dbValue : ymlValue;
        } catch (Exception e) {
            log.debug("Read system config from DB failed for key {}, fallback to yml", key);
            return ymlValue;
        }
    }

    public String getEffectiveAppId() {
        return resolve("alipay.app_id", appId);
    }

    public String getEffectivePrivateKey() {
        return resolve("alipay.private_key", privateKey);
    }

    public String getEffectiveAlipayPublicKey() {
        return resolve("alipay.alipay_public_key", alipayPublicKey);
    }

    public String getEffectiveGateway() {
        return resolve("alipay.gateway", gateway);
    }

    public String getEffectiveNotifyUrl() {
        return resolve("alipay.notify_url", notifyUrl);
    }

    public String getEffectiveReturnUrl() {
        return resolve("alipay.return_url", returnUrl);
    }

    @Bean
    public AlipayClient alipayClient() {
        String effAppId = getEffectiveAppId();
        String effPrivateKey = getEffectivePrivateKey();
        String effAlipayPublicKey = getEffectiveAlipayPublicKey();
        String effGateway = getEffectiveGateway();

        log.info("Initializing AlipayClient, appId={}, gateway={}", effAppId != null ? "configured" : "empty", effGateway);

        if (!isConfigured()) {
            log.warn("Alipay not configured, AlipayClient will be initialized with empty values");
        }

        return new DefaultAlipayClient(
                effGateway != null ? effGateway : "https://openapi-sandbox.dl.alipaydev.com/gateway.do",
                effAppId != null ? effAppId : "",
                effPrivateKey != null ? effPrivateKey : "",
                "json",
                "UTF-8",
                effAlipayPublicKey != null ? effAlipayPublicKey : "",
                signType != null ? signType : "RSA2"
        );
    }

    public boolean isConfigured() {
        String effAppId = getEffectiveAppId();
        String effPrivateKey = getEffectivePrivateKey();
        String effAlipayPublicKey = getEffectiveAlipayPublicKey();
        return effAppId != null && !effAppId.trim().isEmpty()
                && effPrivateKey != null && !effPrivateKey.trim().isEmpty()
                && effAlipayPublicKey != null && !effAlipayPublicKey.trim().isEmpty();
    }
}
