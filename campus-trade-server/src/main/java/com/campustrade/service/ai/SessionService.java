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
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return new ArrayList<>();
        }
        if (cached instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) cached);
        }
        return new ArrayList<>();
    }

    public void addMessage(String sessionId, String role, String content) {
        String key = SESSION_PREFIX + sessionId;
        List<Map<String, Object>> history = getHistory(sessionId);
        history.add(Map.of("role", role, "content", content));
        while (history.size() > MAX_HISTORY * 2) {
            history.remove(0);
        }
        redisTemplate.opsForValue().set(key, history, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
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