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
    private static final String SUMMARY_PREFIX = "ai:session:summary:";
    private static final String SUMMARY_VEC_PREFIX = "ai:session:summary:vec:";
    private static final String PREFS_PREFIX = "ai:session:prefs:";
    private static final int MAX_CONTEXT_TOKENS = 4000;
    private static final int SHORT_TERM_KEEP = 10;
    private static final int MAX_SUMMARY_LENGTH = 2000;

    @Value("${ai.max-history:20}")
    private int maxHistory;

    @Value("${ai.session-ttl-hours:168}")
    private long sessionTtlHours;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DeepSeekClient deepSeekClient;

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
        addMessagePair(sessionId, userMessage, assistantMessage, null, null);
    }

    public void addMessagePair(String sessionId, String userMessage, String assistantMessage,
            List<Map<String, Object>> thinkingSteps, List<Map<String, Object>> toolCalls) {
        String key = SESSION_PREFIX + sessionId;
        try {
            extractAndSavePreferences(sessionId, userMessage);
            long now = System.currentTimeMillis();
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            userMsg.put("timestamp", now);
            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", assistantMessage);
            assistantMsg.put("timestamp", now);
            if (thinkingSteps != null && !thinkingSteps.isEmpty()) assistantMsg.put("thinkingSteps", thinkingSteps);
            if (toolCalls != null && !toolCalls.isEmpty()) assistantMsg.put("toolCalls", toolCalls);
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

    public void removeLastMessagePair(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        try {
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size != null && size >= 2) {
                stringRedisTemplate.opsForList().rightPop(key);
                stringRedisTemplate.opsForList().rightPop(key);
            }
        } catch (Exception e) {
            log.error("Failed to remove last message pair: sessionId={}", sessionId, e);
        }
    }

    public void saveSummary(String sessionId, String summary) {
        try {
            String existing = getLongTermMemory(sessionId);
            String combined = existing != null ? existing + "\n" + summary : summary;
            if (combined.length() > MAX_SUMMARY_LENGTH) {
                combined = combined.substring(combined.length() - MAX_SUMMARY_LENGTH);
                int newline = combined.indexOf('\n');
                if (newline >= 0) combined = combined.substring(newline + 1);
            }
            stringRedisTemplate.opsForValue().set(SUMMARY_PREFIX + sessionId, combined);
            stringRedisTemplate.expire(SUMMARY_PREFIX + sessionId, sessionTtlHours, TimeUnit.HOURS);
            saveSummaryChunkEmbedding(sessionId, summary);
        } catch (Exception e) {
            log.error("Failed to save summary", e);
        }
    }

    private void saveSummaryChunkEmbedding(String sessionId, String summary) {
        if (!deepSeekClient.isEmbeddingAvailable()) return;
        try {
            float[] emb = deepSeekClient.embedding(summary);
            if (emb == null) return;
            String vecKey = SUMMARY_VEC_PREFIX + sessionId;
            String existing = stringRedisTemplate.opsForValue().get(vecKey);
            List<Map<String, Object>> chunks = new ArrayList<>();
            if (existing != null && !existing.isEmpty()) {
                chunks = objectMapper.readValue(existing, new TypeReference<List<Map<String, Object>>>() {});
            }
            if (chunks.size() >= 20) chunks.remove(0);
            Map<String, Object> chunk = new HashMap<>();
            chunk.put("text", summary);
            chunk.put("emb", emb);
            chunks.add(chunk);
            stringRedisTemplate.opsForValue().set(vecKey, objectMapper.writeValueAsString(chunks));
            stringRedisTemplate.expire(vecKey, sessionTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to save summary embedding: {}", e.getMessage());
        }
    }

    private String getRelevantSummaryContext(String sessionId, String userMessage) {
        if (!deepSeekClient.isEmbeddingAvailable()) {
            return getLongTermMemory(sessionId);
        }
        try {
            String vecKey = SUMMARY_VEC_PREFIX + sessionId;
            String existing = stringRedisTemplate.opsForValue().get(vecKey);
            if (existing == null || existing.isEmpty()) {
                return getLongTermMemory(sessionId);
            }
            List<Map<String, Object>> chunks = objectMapper.readValue(existing, new TypeReference<List<Map<String, Object>>>() {});
            if (chunks.isEmpty()) return null;
            float[] queryEmb = deepSeekClient.embedding(userMessage);
            if (queryEmb == null) return getLongTermMemory(sessionId);
            List<double[]> scored = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Object embObj = chunks.get(i).get("emb");
                if (embObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Number> embList = (List<Number>) embObj;
                    float[] emb = new float[embList.size()];
                    for (int j = 0; j < embList.size(); j++) emb[j] = embList.get(j).floatValue();
                    double score = DeepSeekClient.cosineSim(queryEmb, emb);
                    scored.add(new double[]{i, score});
                }
            }
            scored.sort((a, b) -> Double.compare(b[1], a[1]));
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (double[] entry : scored) {
                if (entry[1] < 0.3 || count >= 3) break;
                String text = (String) chunks.get((int) entry[0]).get("text");
                if (sb.length() > 0) sb.append("\n");
                sb.append(text);
                count++;
            }
            return sb.length() > 0 ? sb.toString() : getLongTermMemory(sessionId);
        } catch (Exception e) {
            log.warn("Failed to retrieve relevant summary context: {}", e.getMessage());
            return getLongTermMemory(sessionId);
        }
    }

    public List<Map<String, Object>> buildMessages(String sessionId, String systemPrompt, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        String longTermMemory = getRelevantSummaryContext(sessionId, userMessage);
        if (longTermMemory != null && !longTermMemory.isEmpty()) {
            Map<String, Object> memoryMsg = new HashMap<>();
            memoryMsg.put("role", "system");
            memoryMsg.put("content", "之前的对话摘要：\n" + longTermMemory);
            messages.add(memoryMsg);
        }

        String prefs = getPreferences(sessionId);
        if (prefs != null && !prefs.isEmpty()) {
            Map<String, Object> prefsMsg = new HashMap<>();
            prefsMsg.put("role", "system");
            prefsMsg.put("content", "用户偏好信息：\n" + prefs);
            messages.add(prefsMsg);
        }

        List<Map<String, Object>> history = getHistory(sessionId);
        int shortTermStart = Math.max(0, history.size() - SHORT_TERM_KEEP * 2);
        for (Map<String, Object> msg : truncateByTokens(history.subList(shortTermStart, history.size()), MAX_CONTEXT_TOKENS)) {
            Map<String, Object> clean = new HashMap<>();
            clean.put("role", msg.get("role"));
            clean.put("content", msg.get("content"));
            messages.add(clean);
        }
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        return messages;
    }

    public String getLongTermMemory(String sessionId) {
        try {
            return stringRedisTemplate.opsForValue().get(SUMMARY_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("Failed to get long-term memory: {}", e.getMessage());
            return null;
        }
    }

    public String getPreferences(String sessionId) {
        try {
            return stringRedisTemplate.opsForValue().get(PREFS_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("Failed to get preferences: {}", e.getMessage());
            return null;
        }
    }

    public void savePreferences(String sessionId, String prefs) {
        try {
            stringRedisTemplate.opsForValue().set(PREFS_PREFIX + sessionId, prefs);
            stringRedisTemplate.expire(PREFS_PREFIX + sessionId, sessionTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to save preferences: {}", e.getMessage());
        }
    }

    private static final String[] CATEGORY_KEYWORDS = {"二手书", "教材", "电子产品", "电脑", "手机", "服装", "自行车", "宿舍用品", "文具", "运动器材"};
    private static final String[] PRICE_KEYWORDS = {"便宜", "低价", "实惠", "免费", "50元以下", "100元以下", "200元以下"};

    public void extractAndSavePreferences(String sessionId, String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) return;
        try {
            String lower = userMessage.toLowerCase();
            StringBuilder newPrefs = new StringBuilder();
            for (String cat : CATEGORY_KEYWORDS) {
                if (lower.contains(cat.toLowerCase())) {
                    newPrefs.append("关注类别:").append(cat).append(" ");
                }
            }
            for (String price : PRICE_KEYWORDS) {
                if (lower.contains(price.toLowerCase())) {
                    newPrefs.append("价格偏好:").append(price).append(" ");
                }
            }
            if (newPrefs.length() == 0) return;
            String existing = getPreferences(sessionId);
            String combined = existing != null ? existing + " " + newPrefs : newPrefs.toString();
            String[] parts = combined.split(" ");
            java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>(java.util.Arrays.asList(parts));
            StringBuilder result = new StringBuilder();
            int count = 0;
            for (String p : unique) {
                if (p.isEmpty()) continue;
                if (count > 0) result.append(" ");
                result.append(p);
                count++;
                if (count >= 10) break;
            }
            savePreferences(sessionId, result.toString());
        } catch (Exception e) {
            log.debug("Failed to extract preferences: {}", e.getMessage());
        }
    }

    public boolean shouldSummarize(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null) return false;
        if (size > SHORT_TERM_KEEP * 2L + 4) return true;
        int totalTokens = 0;
        List<Map<String, Object>> history = getHistory(sessionId);
        for (Map<String, Object> msg : history) {
            String content = (String) msg.get("content");
            totalTokens += estimateTokens(content != null ? content : "");
        }
        return totalTokens > MAX_CONTEXT_TOKENS * 0.7;
    }

    public String prepareSummaryContext(String sessionId, String summaryPrompt) {
        String key = SESSION_PREFIX + sessionId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size <= SHORT_TERM_KEEP * 2L + 4) return null;

        List<Map<String, Object>> allHistory = getHistory(sessionId);
        int keepCount = Math.min(SHORT_TERM_KEEP * 2, allHistory.size());
        List<Map<String, Object>> toSummarize = allHistory.subList(0, allHistory.size() - keepCount);

        StringBuilder sb = new StringBuilder(summaryPrompt + "\n\n");
        for (Map<String, Object> msg : toSummarize) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        return sb.toString();
    }

    public void applySummary(String sessionId, String summary) {
        try {
            String existing = getLongTermMemory(sessionId);
            String combined = existing != null ? existing + "\n" + summary : summary;
            if (combined.length() > MAX_SUMMARY_LENGTH) {
                combined = combined.substring(combined.length() - MAX_SUMMARY_LENGTH);
                int newline = combined.indexOf('\n');
                if (newline >= 0) combined = combined.substring(newline + 1);
            }
            stringRedisTemplate.opsForValue().set(SUMMARY_PREFIX + sessionId, combined);
            stringRedisTemplate.expire(SUMMARY_PREFIX + sessionId, sessionTtlHours, TimeUnit.HOURS);
            saveSummaryChunkEmbedding(sessionId, summary);

            String key = SESSION_PREFIX + sessionId;
            List<Map<String, Object>> allHistory = getHistory(sessionId);
            int keepCount = Math.min(SHORT_TERM_KEEP * 2, allHistory.size());
            List<Map<String, Object>> toKeep = allHistory.subList(Math.max(0, allHistory.size() - keepCount), allHistory.size());

            stringRedisTemplate.delete(key);
            for (Map<String, Object> msg : toKeep) {
                String json = objectMapper.writeValueAsString(msg);
                stringRedisTemplate.opsForList().rightPush(key, json);
            }
            stringRedisTemplate.expire(key, sessionTtlHours, TimeUnit.HOURS);
            log.info("Session {} summarized: kept {} recent messages, long-term memory updated", sessionId, toKeep.size());
        } catch (Exception e) {
            log.error("Failed to apply summary: {}", sessionId, e);
        }
    }

    private List<Map<String, Object>> truncateByTokens(List<Map<String, Object>> history, int maxTokens) {
        int totalTokens = 0;
        int cutoff = 0;
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
