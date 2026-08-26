package com.campustrade.health;

import com.campustrade.service.ai.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;


@Slf4j
@Component("ai")
public class AiHealthIndicator implements HealthIndicator {

    private static final long CACHE_TTL_MS = 60_000L;

    @Autowired
    private DeepSeekClient deepSeekClient;

    private volatile Health cachedHealth;
    private volatile long lastCheckTime;

    public void clearCache() {
        cachedHealth = null;
        lastCheckTime = 0;
    }

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
            boolean healthy = deepSeekClient.checkApiHealth();
            if (healthy) {
                return Health.up()
                        .withDetail("model", deepSeekClient.getModel())
                        .withDetail("status", "responsive")
                        .build();
            } else {
                return Health.down().withDetail("reason", "AI API /models check failed").build();
            }
        } catch (Exception e) {
            log.warn("AI health check failed: {}", e.getMessage());
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}