package com.campustrade.service;

import com.campustrade.common.Result;
import com.campustrade.entity.SystemConfig;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    Result<List<SystemConfig>> listAll();

    Result<Map<String, String>> getAlipayConfig();

    Result<Void> updateConfig(String configKey, String configValue);

    Result<Void> batchUpdate(List<SystemConfig> configs);

    String getConfigValue(String key);

    String getDecryptedValue(String key);
}