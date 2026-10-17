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

    @Autowired
    private DeepSeekClient deepSeekClient;

    public void clearCache() {
    }

    @Override
    public Health health() {
        if (!deepSeekClient.isEnabled()) {
            return Health.down().withDetail("reason", "AI service disabled or API key not configured").build();
        }
        return doHealthCheck();
    }

    private Health doHealthCheck() {
        if (deepSeekClient.isCircuitBreakerOpen()) {
            return Health.down().withDetail("reason", "熔断保护中，API服务不可用").build();
        }
        try {
            boolean healthy = deepSeekClient.checkApiHealth();
            if (healthy) {
                return Health.up()
                        .withDetail("model", deepSeekClient.getEffectiveModel())
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