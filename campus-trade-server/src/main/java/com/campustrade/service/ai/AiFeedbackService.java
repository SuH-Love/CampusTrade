package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class AiFeedbackService {

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("aiAgentExecutor")
    private ThreadPoolTaskExecutor aiAgentExecutor;

    public void analyzeNegativeFeedbackAsync(String userMessage, String aiResponse, String userFeedback, String sessionId) {
        if (!deepSeekClient.isEnabled()) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> msgs = new ArrayList<>();
                Map<String, Object> sys = new HashMap<>();
                sys.put("role", "system");
                sys.put("content", "你是一个AI回复质量分析专家。用户对AI的回复给了差评，请分析可能的原因并给出改进建议。用JSON格式返回：{\"reasons\":[\"原因1\",\"原因2\"],\"suggestion\":\"改进建议\"}");
                msgs.add(sys);
                Map<String, Object> user = new HashMap<>();
                user.put("role", "user");
                user.put("content", "用户问题: " + (userMessage != null ? userMessage.substring(0, Math.min(200, userMessage.length())) : "") +
                        "\nAI回复: " + (aiResponse != null ? aiResponse.substring(0, Math.min(500, aiResponse.length())) : "") +
                        "\n用户反馈: " + (userFeedback != null ? userFeedback : "无"));
                msgs.add(user);
                String analysis = deepSeekClient.chat(msgs);
                if (analysis != null && !analysis.isEmpty()) {
                    String key = "ai:feedback:analysis:" + sessionId + ":" + System.currentTimeMillis();
                    stringRedisTemplate.opsForValue().set(key, analysis, 7, java.util.concurrent.TimeUnit.DAYS);
                    log.info("Negative feedback analyzed for session {}: {}", sessionId, analysis.substring(0, Math.min(200, analysis.length())));
                }
            } catch (Exception e) {
                log.warn("Failed to analyze negative feedback: {}", e.getMessage());
            }
        }, aiAgentExecutor);
    }

    public Map<String, Object> getFeedbackList(int page, int size, Integer minRating, Integer maxRating) {
        int offset = (page - 1) * size;
        StringBuilder sql = new StringBuilder(
            "SELECT f.*, u.username FROM t_ai_feedback f LEFT JOIN t_user u ON f.user_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (minRating != null) { sql.append(" AND f.rating >= ?"); params.add(minRating); }
        if (maxRating != null) { sql.append(" AND f.rating <= ?"); params.add(maxRating); }
        sql.append(" ORDER BY f.create_time DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        String countSql = "SELECT COUNT(*) FROM t_ai_feedback f WHERE 1=1" +
            (minRating != null ? " AND f.rating >= " + minRating : "") +
            (maxRating != null ? " AND f.rating <= " + maxRating : "");
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}