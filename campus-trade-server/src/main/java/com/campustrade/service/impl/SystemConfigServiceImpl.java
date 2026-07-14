package com.campustrade.service.impl;

import com.campustrade.common.Result;
import com.campustrade.entity.SystemConfig;
import com.campustrade.mapper.SystemConfigMapper;
import com.campustrade.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String AES_KEY = "CampusTrade2026!";
    private static final Set<String> SENSITIVE_KEYS = Set.of("alipay.private_key", "alipay.alipay_public_key");

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public Result<List<SystemConfig>> listAll() {
        List<SystemConfig> list = systemConfigMapper.selectAll();
        for (SystemConfig config : list) {
            if (SENSITIVE_KEYS.contains(config.getConfigKey()) && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                config.setConfigValue("******");
            }
        }
        return Result.success(list);
    }

    @Override
    public Result<Map<String, String>> getAlipayConfig() {
        Map<String, String> result = new LinkedHashMap<>();
        String[] keys = {"alipay.app_id", "alipay.private_key", "alipay.alipay_public_key", "alipay.gateway", "alipay.notify_url", "alipay.return_url"};
        for (String key : keys) {
            String value = getDecryptedValue(key);
            if (SENSITIVE_KEYS.contains(key) && value != null && !value.isEmpty()) {
                result.put(key, "******");
            } else {
                result.put(key, value != null ? value : "");
            }
        }
        return Result.success(result);
    }

    @Override
    public Result<Void> updateConfig(String configKey, String configValue) {
        SystemConfig existing = systemConfigMapper.selectByKey(configKey);
        if (existing == null) {
            return Result.error(404, "配置项不存在: " + configKey);
        }

        String valueToStore = configValue;
        if (SENSITIVE_KEYS.contains(configKey) && !"******".equals(configValue) && !configValue.isEmpty()) {
            valueToStore = encrypt(configValue);
        } else if ("******".equals(configValue)) {
            return Result.success();
        }

        existing.setConfigValue(valueToStore);
        systemConfigMapper.updateByKey(existing);
        cache.remove(configKey);
        log.info("System config updated: {}", configKey);
        return Result.success();
    }

    @Override
    public Result<Void> batchUpdate(List<SystemConfig> configs) {
        for (SystemConfig config : configs) {
            if (config.getConfigKey() != null) {
                updateConfig(config.getConfigKey(), config.getConfigValue());
            }
        }
        return Result.success();
    }

    @Override
    public String getConfigValue(String key) {
        String cached = cache.get(key);
        if (cached != null) return cached;

        SystemConfig config = systemConfigMapper.selectByKey(key);
        String value = config != null ? config.getConfigValue() : null;
        if (value != null) cache.put(key, value);
        return value;
    }

    @Override
    public String getDecryptedValue(String key) {
        String value = getConfigValue(key);
        if (value == null || value.isEmpty()) return value;
        if (SENSITIVE_KEYS.contains(key)) {
            try {
                return decrypt(value);
            } catch (Exception e) {
                log.warn("Decrypt failed for key {}, return raw value", key);
                return value;
            }
        }
        return value;
    }

    private String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES encrypt failed", e);
            return plainText;
        }
    }

    private String decrypt(String cipherText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }
}