package com.campustrade.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private static final int MAX_CONTEXT_TOKENS = 4000;

    @Value("${ai.max-history:20}")
    private int maxHistory;

    @Value("${ai.session-ttl-hours:168}")
    private long sessionTtlHours;

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
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", role);
            msg.put("content", content);
            msg.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(msg);
            stringRedisTemplate.opsForList().rightPush(key, json);
            stringRedisTemplate.opsForList().trim(key, -maxHistory * 2L, -1);
            stringRedisTemplate.expire(key, sessionTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to add session message: sessionId={}, role={}", sessionId, role, e);
        }
    }

    public void addMessagePair(String sessionId, String userMessage, String assistantMessage) {
        String key = SESSION_PREFIX + sessionId;
        try {
            long now = System.currentTimeMillis();
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            userMsg.put("timestamp", now);
            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", assistantMessage);
            assistantMsg.put("timestamp", now);
            String userJson = objectMapper.writeValueAsString(userMsg);
            String assistantJson = objectMapper.writeValueAsString(assistantMsg);
            stringRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize(key);
                connection.rPush(rawKey, stringRedisTemplate.getStringSerializer().serialize(userJson));
                connection.rPush(rawKey, stringRedisTemplate.getStringSerializer().serialize(assistantJson));
                connection.lTrim(rawKey, -maxHistory * 2L, -1);
                connection.expire(rawKey, sessionTtlHours * 3600);
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to add message pair: sessionId={}", sessionId, e);
        }
    }

    public void clearSession(String sessionId) {
        stringRedisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    public void saveSummary(String sessionId, String summary) {
        String key = SESSION_PREFIX + sessionId;
        try {
            Map<String, Object> summaryMsg = new HashMap<>();
            summaryMsg.put("role", "system");
            summaryMsg.put("content", "之前的对话摘要：\n" + summary);
            String json = objectMapper.writeValueAsString(summaryMsg);
            stringRedisTemplate.opsForList().set(key, 0, json);
        } catch (Exception e) {
            log.error("Failed to save summary", e);
        }
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

    public boolean shouldSummarize(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Long size = stringRedisTemplate.opsForList().size(key);
        return size != null && size > maxHistory * 2L - 4;
    }

    public String summarizeAndCompact(String sessionId, String summaryPrompt) {
        String key = SESSION_PREFIX + sessionId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size <= maxHistory * 2L - 4) return null;

        List<Map<String, Object>> allHistory = getHistory(sessionId);
        int keepCount = Math.min(10, allHistory.size());
        List<Map<String, Object>> toSummarize = allHistory.subList(0, allHistory.size() - keepCount);
        List<Map<String, Object>> toKeep = allHistory.subList(allHistory.size() - keepCount, allHistory.size());

        StringBuilder sb = new StringBuilder(summaryPrompt + "\n\n");
        for (Map<String, Object> msg : toSummarize) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }

        Map<String, Object> summaryMsg = new HashMap<>();
        summaryMsg.put("role", "system");
        summaryMsg.put("content", "之前的对话摘要：\n" + sb.toString());

        try {
            String summaryJson = objectMapper.writeValueAsString(summaryMsg);
            List<String> keepJsons = new ArrayList<>(toKeep.size());
            for (Map<String, Object> msg : toKeep) {
                keepJsons.add(objectMapper.writeValueAsString(msg));
            }

            stringRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize(key);
                connection.del(rawKey);
                connection.rPush(rawKey, stringRedisTemplate.getStringSerializer().serialize(summaryJson));
                for (String json : keepJsons) {
                    connection.rPush(rawKey, stringRedisTemplate.getStringSerializer().serialize(json));
                }
                connection.expire(rawKey, sessionTtlHours * 3600);
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to summarize and compact session: {}", sessionId, e);
        }
        return sb.toString();
    }

    private List<Map<String, Object>> truncateByTokens(List<Map<String, Object>> history, int maxTokens) {
        int totalTokens = 0;
        int cutoff = history.size();
        for (int i = history.size() - 1; i >= 0; i--) {
            String content = (String) history.get(i).get("content");
            int tokens = content != null ? estimateTokens(content) : 0;
            totalTokens += tokens;
            if (totalTokens > maxTokens) {
                cutoff = i + 1;
                break;
            }
        }
        return history.subList(cutoff, history.size());
    }

    private int estimateTokens(String text) {
        int chineseChars = 0, otherChars = 0;
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) chineseChars++;
            else otherChars++;
        }
        return (int) Math.ceil(chineseChars * 1.5 + otherChars * 0.25);
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
