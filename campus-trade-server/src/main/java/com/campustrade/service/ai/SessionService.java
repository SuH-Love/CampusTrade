package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SessionService {

    private static final String SESSION_PREFIX = "ai:session:";
    private static final int MAX_HISTORY = 20;
    private static final long SESSION_TTL_SECONDS = 1800;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getHistory(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    public void addMessage(String sessionId, String role, String content) {
        String key = SESSION_PREFIX + sessionId;
        Map<String, Object> message = Map.of("role", role, "content", content);
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.opsForList().trim(key, -MAX_HISTORY * 2, -1);
        redisTemplate.expire(key, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void clearSession(String sessionId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    public List<Map<String, Object>> buildMessages(String sessionId, String systemPrompt, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(getHistory(sessionId));
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    public List<Map<String, Object>> buildMessages(String systemPrompt, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }
}
