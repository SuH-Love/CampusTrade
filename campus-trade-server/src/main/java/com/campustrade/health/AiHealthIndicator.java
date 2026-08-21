package com.campustrade.health;

import com.campustrade.service.ai.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("ai")
public class AiHealthIndicator implements HealthIndicator {

    private static final long CACHE_TTL_MS = 60_000L;

    @Autowired
    private DeepSeekClient deepSeekClient;

    private volatile Health cachedHealth;
    private volatile long lastCheckTime;

    @Override
    public Health health() {
        if (!deepSeekClient.isEnabled()) {
            return Health.down().withDetail("reason", "AI service disabled or API key not configured").build();
        }

        long now = System.currentTimeMillis();
        if (cachedHealth != null && (now - lastCheckTime) < CACHE_TTL_MS) {
            return cachedHealth;
        }

        synchronized (this) {
            if (cachedHealth != null && (now - lastCheckTime) < CACHE_TTL_MS) {
                return cachedHealth;
            }
            cachedHealth = doHealthCheck();
            lastCheckTime = now;
            return cachedHealth;
        }
    }

    private Health doHealthCheck() {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", "ping");
            messages.add(msg);

            String response = deepSeekClient.chat(messages);
            if (response != null && !response.isEmpty()) {
                return Health.up()
                        .withDetail("model", deepSeekClient.getModel())
                        .withDetail("status", "responsive")
                        .build();
            } else {
                return Health.down().withDetail("reason", "Empty response from AI").build();
            }
        } catch (Exception e) {
            log.warn("AI health check failed: {}", e.getMessage());
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}