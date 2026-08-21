package com.campustrade.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SessionService {

    private static final String SESSION_PREFIX = "ai:session:";
    private static final int MAX_HISTORY = 20;
    private static final long SESSION_TTL_SECONDS = 1800;
    private static final int MAX_CONTEXT_TOKENS = 4000;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> getHistory(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        List<String> raw = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>(raw.size());
        for (String json : raw) {
            try {
                Map<String, Object> msg = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                result.add(msg);
            } catch (Exception e) {
                log.warn("Failed to parse session message: {}", json, e);
            }
        }
        return result;
    }

    public void addMessage(String sessionId, String role, String content) {
        String key = SESSION_PREFIX + sessionId;
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("role", role);
            message.put("content", content);
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.opsForList().rightPush(key, json);
            stringRedisTemplate.opsForList().trim(key, -MAX_HISTORY * 2, -1);
            stringRedisTemplate.expire(key, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to add session message: sessionId={}, role={}", sessionId, role, e);
        }
    }

    public void clearSession(String sessionId) {
        stringRedisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    public List<Map<String, Object>> buildMessages(String sessionId, String systemPrompt, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        messages.addAll(truncateByTokens(getHistory(sessionId), MAX_CONTEXT_TOKENS));
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        return messages;
    }

    private List<Map<String, Object>> truncateByTokens(List<Map<String, Object>> history, int maxTokens) {
        int totalTokens = 0;
        int cutoff = history.size();
        for (int i = history.size() - 1; i >= 0; i--) {
            String content = (String) history.get(i).get("content");
            int tokens = content != null ? content.length() / 2 + 1 : 0;
            totalTokens += tokens;
            if (totalTokens > maxTokens) {
                cutoff = i + 1;
                break;
            }
        }
        return history.subList(cutoff, history.size());
    }

    public List<Map<String, Object>> buildMessages(String systemPrompt, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        return messages;
    }
}
