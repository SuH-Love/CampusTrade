package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class AiRateLimiter {

    private static final String RATE_KEY_PREFIX = "ai:rate:";

    @Value("${ai.rate-limit.per-minute:10}")
    private int perMinute;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> RATE_SCRIPT;

    static {
        RATE_SCRIPT = new DefaultRedisScript<>();
        RATE_SCRIPT.setScriptText(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], 60) end " +
            "return current"
        );
        RATE_SCRIPT.setResultType(Long.class);
    }

    public boolean tryAcquire(String userId) {
        String key = RATE_KEY_PREFIX + userId;
        Long current = stringRedisTemplate.execute(RATE_SCRIPT, Collections.singletonList(key));
        if (current != null && current > perMinute) {
            log.warn("AI rate limit exceeded for user: {}, count: {}/{}", userId, current, perMinute);
            return false;
        }
        return true;
    }

    public int getPerMinute() { return perMinute; }
}
