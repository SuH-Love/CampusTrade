package com.campustrade.service.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DeepSeekClient {

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.timeout-ms:120000}")
    private int timeoutMs;

    @Value("${ai.deepseek.stream-timeout-ms:180000}")
    private int streamTimeoutMs;

    @Value("${file.upload.path:/data/uploads}")
    private String uploadBasePath;

    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.deepseek.max-concurrent:5}")
    private int maxConcurrent;

    @Value("${ai.deepseek.max-tokens:4096}")
    private int maxTokens;

    @Value("${ai.fallback.api-key:}")
    private String fallbackApiKey;

    @Value("${ai.fallback.base-url:}")
    private String fallbackBaseUrl;

    @Value("${ai.fallback.model:}")
    private String fallbackModel;

    @Value("${ai.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${ai.embedding.base-url:}")
    private String embeddingBaseUrl;

    @Value("${ai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${ai.routing.enabled:false}")
    private boolean routingEnabled;

    @Value("${ai.routing.reasoner-model:deepseek-reasoner}")
    private String reasonerModel;

    @Value("${ai.vision.model:}")
    private String visionModel;

    @Autowired
    @Qualifier("aiTaskExecutor")
    private ThreadPoolTaskExecutor aiTaskExecutor;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_KEY_APIKEY = "ai:config:apikey";
    private static final String REDIS_KEY_MODEL = "ai:config:model";
    private static final String REDIS_KEY_BASEURL = "ai:config:baseUrl";
    private static final String REDIS_KEY_EMB_APIKEY = "ai:config:emb:apikey";
    private static final String REDIS_KEY_EMB_BASEURL = "ai:config:emb:baseUrl";
    private static final String REDIS_KEY_EMB_MODEL = "ai:config:emb:model";
    private static final String REDIS_KEY_ROUTING_ENABLED = "ai:config:routing:enabled";
    private static final String REDIS_KEY_ROUTING_REASONER = "ai:config:routing:reasoner";
    private static final String REDIS_KEY_VISION_MODEL = "ai:config:vision:model";

    private static final String REDIS_KEY_STAT_API_CALLS = "ai:stats:api_calls";
    private static final String REDIS_KEY_STAT_SUCCESS = "ai:stats:success_calls";
    private static final String REDIS_KEY_STAT_FAILURE = "ai:stats:failure_calls";
    private static final String REDIS_KEY_CB_STATE = "ai:cb:state";
    private static final String REDIS_KEY_STAT_TOKENS = "ai:stats:total_tokens";
    private static final String REDIS_KEY_STAT_LATENCY_SUM = "ai:stats:latency_sum";
    private static final String REDIS_KEY_STAT_LATENCY_COUNT = "ai:stats:latency_count";


    private Counter requestCounter;
    private Counter errorCounter;
    private Timer latencyTimer;
    private Semaphore concurrencyLimit;
    private volatile String currentApiKey;
    private volatile String currentModel;
    private volatile String currentBaseUrl;
    private volatile String currentEmbApiKey;
    private volatile String currentEmbBaseUrl;
    private volatile String currentEmbModel;
    private volatile boolean currentRoutingEnabled;
    private volatile String currentReasonerModel;
    private volatile String currentVisionModel;

    private final java.util.concurrent.atomic.AtomicInteger cbFailureCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicBoolean halfOpenProbe = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long cbOpenTime = 0;
    private volatile boolean cbOpen = false;
    private volatile String lastFailureReason = null;
    private volatile java.util.Map<String, Object> cachedApiStatus = null;
    private volatile long apiStatusCacheTime = 0;
    private static final long API_STATUS_CACHE_TTL_MS = 10_000L;
    private static final int CB_FAILURE_THRESHOLD = 5;
    private static final long CB_RECOVERY_TIMEOUT_MS = 15000;

    private final java.util.concurrent.atomic.AtomicLong totalTokens = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong successApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong failureApiCalls = new java.util.concurrent.atomic.AtomicLong(0);

    private void incrRedisStat(String key, java.util.concurrent.atomic.AtomicLong local) {
        local.incrementAndGet();
        try { stringRedisTemplate.opsForValue().increment(key); } catch (Exception ignored) {}
    }

    private void incrRedisStatBy(String key, java.util.concurrent.atomic.AtomicLong local, long delta) {
        local.addAndGet(delta);
        try { stringRedisTemplate.opsForValue().increment(key, delta); } catch (Exception ignored) {}
    }

    private long getRedisStat(String key, java.util.concurrent.atomic.AtomicLong local) {
        try {
            String v = stringRedisTemplate.opsForValue().get(key);
            if (v != null) return Long.parseLong(v);
        } catch (Exception ignored) {}
        return local.get();
    }

    private void recordLatency(long durationNanos) {
        try {
            long ms = durationNanos / 1_000_000;
            stringRedisTemplate.opsForValue().increment(REDIS_KEY_STAT_LATENCY_SUM, ms);
            stringRedisTemplate.opsForValue().increment(REDIS_KEY_STAT_LATENCY_COUNT);
        } catch (Exception ignored) {}
    }

    private boolean cbAllowRequest() {
        if (!cbOpen) return true;
        if (System.currentTimeMillis() - cbOpenTime > CB_RECOVERY_TIMEOUT_MS) {
            if (halfOpenProbe.compareAndSet(false, true)) {
                log.info("Circuit breaker entering half-open state, probe granted");
                return true;
            }
            return false;
        }
        return false;
    }

    private void cbRecordSuccess() {
        if (cbFailureCount.get() > 0 || cbOpen) {
            log.info("Circuit breaker closed (recovered)");
        }
        cbFailureCount.set(0);
        halfOpenProbe.set(false);
        cbOpen = false;
        lastFailureReason = null;
        cachedApiStatus = null;
        try { stringRedisTemplate.delete(REDIS_KEY_CB_STATE); } catch (Exception ignored) {}
        notifyCbStateChange("closed");
    }

    private void cbRecordFailure() {
        cbRecordFailure(null);
    }

    private String extractFailureReason(int code, String body) {
        if (code == 402) return "余额不足";
        if (code == 401) return "API Key无效或已过期";
        if (code == 429) return "调用频率超限";
        if (code >= 500) return "API服务端错误(HTTP " + code + ")";
        return "HTTP " + code;
    }

    private String extractExceptionReason(Exception e) {
        String msg = e.getMessage();
        if (msg == null) msg = e.getClass().getSimpleName();
        if (msg.contains("Connect") || msg.contains("connect") || msg.contains("timeout") || msg.contains("Timeout")) return "网络连接超时";
        msg = msg.length() > 80 ? msg.substring(0, 80) : msg;
        return msg;
    }

    private void cbRecordFailure(String reason) {
        if (reason != null) lastFailureReason = reason;
        cachedApiStatus = null;
        halfOpenProbe.set(false);
        int newCount = cbFailureCount.incrementAndGet();
        if (newCount >= CB_FAILURE_THRESHOLD && !cbOpen) {
            cbOpen = true;
            cbOpenTime = System.currentTimeMillis();
            log.warn("Circuit breaker opened after {} consecutive failures", newCount);
        } else if (cbOpen) {
            cbOpenTime = System.currentTimeMillis();
            log.warn("Circuit breaker re-opened after half-open test failure");
        }
        persistCbState();
        notifyCbStateChange(cbOpen ? "open" : "degraded");
    }

    private void persistCbState() {
        try {
            java.util.Map<String, String> state = new java.util.HashMap<>();
            state.put("failCount", String.valueOf(cbFailureCount.get()));
            state.put("open", String.valueOf(cbOpen));
            state.put("openTime", String.valueOf(cbOpenTime));
            state.put("reason", lastFailureReason == null ? "" : lastFailureReason);
            stringRedisTemplate.opsForHash().putAll(REDIS_KEY_CB_STATE, state);
        } catch (Exception e) {
            log.warn("Failed to persist circuit breaker state to Redis", e);
        }
    }

    private void restoreCbState() {
        try {
            java.util.Map<Object, Object> state = stringRedisTemplate.opsForHash().entries(REDIS_KEY_CB_STATE);
            if (state == null || state.isEmpty()) return;
            String fc = (String) state.get("failCount");
            String open = (String) state.get("open");
            String ot = (String) state.get("openTime");
            String reason = (String) state.get("reason");
            if (fc != null) cbFailureCount.set(Integer.parseInt(fc));
            if (ot != null) cbOpenTime = Long.parseLong(ot);
            if (reason != null && !reason.isEmpty()) lastFailureReason = reason;
            if ("true".equals(open)) {
                if (System.currentTimeMillis() - cbOpenTime > CB_RECOVERY_TIMEOUT_MS) {
                    cbOpen = false;
                    cbFailureCount.set(0);
                    lastFailureReason = null;
                    stringRedisTemplate.delete(REDIS_KEY_CB_STATE);
                    log.info("Circuit breaker state expired while offline, reset to closed");
                } else {
                    cbOpen = true;
                    log.info("Circuit breaker state restored from Redis: open, remaining {}s",
                            (CB_RECOVERY_TIMEOUT_MS - (System.currentTimeMillis() - cbOpenTime)) / 1000);
                }
            } else {
                cbOpen = false;
                log.info("Circuit breaker state restored from Redis: failCount={}", cbFailureCount.get());
            }
        } catch (Exception e) {
            log.warn("Failed to restore circuit breaker state from Redis", e);
        }
    }

    private static final Map<String, String> FALLBACK_ANSWERS = new ConcurrentHashMap<>();

    static {
        FALLBACK_ANSWERS.put("faq", "抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。");
        FALLBACK_ANSWERS.put("default", "AI 助手正在休息，请稍后再试。");
    }

    public static class RoutedConfig {
        public final String baseUrl;
        public final String apiKey;
        public final String model;
        public RoutedConfig(String baseUrl, String apiKey, String model) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
        }
    }

    @PostConstruct
    public void init() {
        concurrencyLimit = new Semaphore(maxConcurrent);
        currentApiKey = apiKey;
        currentModel = model;
        currentBaseUrl = baseUrl;
        currentEmbApiKey = embeddingApiKey;
        currentEmbBaseUrl = embeddingBaseUrl;
        currentEmbModel = embeddingModel;
        currentRoutingEnabled = routingEnabled;
        currentReasonerModel = reasonerModel;
        currentVisionModel = visionModel;
        try {
            String savedKey = stringRedisTemplate.opsForValue().get(REDIS_KEY_APIKEY);
            String savedModel = stringRedisTemplate.opsForValue().get(REDIS_KEY_MODEL);
            String savedUrl = stringRedisTemplate.opsForValue().get(REDIS_KEY_BASEURL);
            if (savedKey != null && !savedKey.isEmpty()) currentApiKey = savedKey;
            if (savedModel != null && !savedModel.isEmpty()) currentModel = savedModel;
            if (savedUrl != null && !savedUrl.isEmpty()) currentBaseUrl = savedUrl;
            String savedEmbKey = stringRedisTemplate.opsForValue().get(REDIS_KEY_EMB_APIKEY);
            String savedEmbUrl = stringRedisTemplate.opsForValue().get(REDIS_KEY_EMB_BASEURL);
            String savedEmbModel = stringRedisTemplate.opsForValue().get(REDIS_KEY_EMB_MODEL);
            String savedRoutingEnabled = stringRedisTemplate.opsForValue().get(REDIS_KEY_ROUTING_ENABLED);
            String savedReasoner = stringRedisTemplate.opsForValue().get(REDIS_KEY_ROUTING_REASONER);
            String savedVisionModel = stringRedisTemplate.opsForValue().get(REDIS_KEY_VISION_MODEL);
            if (savedEmbKey != null && !savedEmbKey.isEmpty()) currentEmbApiKey = savedEmbKey;
            if (savedEmbUrl != null && !savedEmbUrl.isEmpty()) currentEmbBaseUrl = savedEmbUrl;
            if (savedEmbModel != null && !savedEmbModel.isEmpty()) currentEmbModel = savedEmbModel;
            if (savedRoutingEnabled != null) currentRoutingEnabled = "true".equals(savedRoutingEnabled);
            if (savedReasoner != null && !savedReasoner.isEmpty()) currentReasonerModel = savedReasoner;
            if (savedVisionModel != null && !savedVisionModel.isEmpty()) currentVisionModel = savedVisionModel;
            String sApi = stringRedisTemplate.opsForValue().get(REDIS_KEY_STAT_API_CALLS);
            String sSuccess = stringRedisTemplate.opsForValue().get(REDIS_KEY_STAT_SUCCESS);
            String sFailure = stringRedisTemplate.opsForValue().get(REDIS_KEY_STAT_FAILURE);
            String sTokens = stringRedisTemplate.opsForValue().get(REDIS_KEY_STAT_TOKENS);
            if (sApi != null) totalApiCalls.set(Long.parseLong(sApi));
            if (sSuccess != null) successApiCalls.set(Long.parseLong(sSuccess));
            if (sFailure != null) failureApiCalls.set(Long.parseLong(sFailure));
            if (sTokens != null) totalTokens.set(Long.parseLong(sTokens));
            log.info("Loaded AI stats from Redis: calls={}, success={}, failure={}, tokens={}",
                    totalApiCalls.get(), successApiCalls.get(), failureApiCalls.get(), totalTokens.get());
            restoreCbState();
        } catch (Exception e) {
            log.warn("Failed to load AI config from Redis, using defaults", e);
        }
        requestCounter = Counter.builder("ai.deepseek.requests.total")
                .description("DeepSeek request count")
                .tag("provider", "deepseek")
                .register(meterRegistry);
        errorCounter = Counter.builder("ai.deepseek.errors.total")
                .description("DeepSeek request error count")
                .tag("provider", "deepseek")
                .register(meterRegistry);
        latencyTimer = Timer.builder("ai.deepseek.latency")
                .description("DeepSeek request latency")
                .tag("provider", "deepseek")
                .register(meterRegistry);
    }

    public void reloadConfig() {
        try {
            String savedKey = stringRedisTemplate.opsForValue().get(REDIS_KEY_APIKEY);
            String savedModel = stringRedisTemplate.opsForValue().get(REDIS_KEY_MODEL);
            String savedUrl = stringRedisTemplate.opsForValue().get(REDIS_KEY_BASEURL);
            if (savedKey != null && !savedKey.isEmpty()) currentApiKey = savedKey;
            if (savedModel != null && !savedModel.isEmpty()) currentModel = savedModel;
            if (savedUrl != null && !savedUrl.isEmpty()) currentBaseUrl = savedUrl;
            String savedRoutingEnabled = stringRedisTemplate.opsForValue().get(REDIS_KEY_ROUTING_ENABLED);
            String savedReasoner = stringRedisTemplate.opsForValue().get(REDIS_KEY_ROUTING_REASONER);
            String savedVisionModel = stringRedisTemplate.opsForValue().get(REDIS_KEY_VISION_MODEL);
            if (savedRoutingEnabled != null) currentRoutingEnabled = "true".equals(savedRoutingEnabled);
            if (savedReasoner != null && !savedReasoner.isEmpty()) currentReasonerModel = savedReasoner;
            if (savedVisionModel != null && !savedVisionModel.isEmpty()) currentVisionModel = savedVisionModel;
            String savedEmbKey = stringRedisTemplate.opsForValue().get(REDIS_KEY_EMB_APIKEY);
            String savedEmbUrl = stringRedisTemplate.opsForValue().get(REDIS_KEY_EMB_BASEURL);
            String savedEmbModel = stringRedisTemplate.opsForValue().get(REDIS_KEY_EMB_MODEL);
            if (savedEmbKey != null && !savedEmbKey.isEmpty()) currentEmbApiKey = savedEmbKey;
            if (savedEmbUrl != null && !savedEmbUrl.isEmpty()) currentEmbBaseUrl = savedEmbUrl;
            if (savedEmbModel != null && !savedEmbModel.isEmpty()) currentEmbModel = savedEmbModel;
            clearEmbeddingAvailableCache();
            log.info("AI config reloaded from Redis: model={}, routingEnabled={}", currentModel, currentRoutingEnabled);
        } catch (Exception e) {
            log.warn("Failed to reload AI config from Redis: {}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return aiEnabled && currentApiKey != null && !currentApiKey.isEmpty();
    }

    public Map<String, Object> getApiStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = getRedisStat(REDIS_KEY_STAT_API_CALLS, totalApiCalls);
        long success = getRedisStat(REDIS_KEY_STAT_SUCCESS, successApiCalls);
        long failure = getRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
        long tokens = getRedisStat(REDIS_KEY_STAT_TOKENS, totalTokens);
        stats.put("totalApiCalls", total);
        stats.put("successApiCalls", success);
        stats.put("failureApiCalls", failure);
        stats.put("totalTokens", tokens);
        stats.put("successRate", total > 0 ? Math.round((double) success / total * 1000) / 10.0 : 0);
        stats.put("failureRate", total > 0 ? Math.round((double) failure / total * 1000) / 10.0 : 0);
        try {
            String sLatencySum = stringRedisTemplate.opsForValue().get(REDIS_KEY_STAT_LATENCY_SUM);
            String sLatencyCount = stringRedisTemplate.opsForValue().get(REDIS_KEY_STAT_LATENCY_COUNT);
            long latencySum = sLatencySum != null ? Long.parseLong(sLatencySum) : 0;
            long latencyCount = sLatencyCount != null ? Long.parseLong(sLatencyCount) : 0;
            if (latencyCount > 0) {
                stats.put("avgLatencyMs", Math.round((double) latencySum / latencyCount * 10) / 10.0);
            } else {
                stats.put("avgLatencyMs", 0);
            }
        } catch (Exception e) {
            stats.put("avgLatencyMs", 0);
        }
        return stats;
    }

    private volatile Map<String, List<float[]>> intentTemplateEmbeddings;
    private static final Map<String, String[]> INTENT_TEMPLATES = new LinkedHashMap<>();
    static {
        INTENT_TEMPLATES.put("reasoning", new String[]{
            "分析销售趋势", "对比这两个商品", "为什么订单被取消",
            "计算我的消费总额", "推荐一些热门商品", "评估这个商品的价格是否合理",
            "统计平台运营数据", "这两个方案有什么区别和优缺点"
        });
        INTENT_TEMPLATES.put("chat", new String[]{
            "帮我查订单", "怎么发布商品", "我的收藏列表",
            "取消这个订单", "确认收货", "修改收货地址",
            "搜索图书类商品", "怎么退款"
        });
    }

    public static double cosineSim(float[] v1, float[] v2) {
        double dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < v1.length && i < v2.length; i++) {
            dot += v1[i] * v2[i]; n1 += v1[i] * v1[i]; n2 += v2[i] * v2[i];
        }
        if (n1 == 0 || n2 == 0) return 0;
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    private synchronized void initIntentTemplateEmbeddings() {
        if (intentTemplateEmbeddings != null) return;
        try {
            Map<String, List<float[]>> result = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : INTENT_TEMPLATES.entrySet()) {
                List<float[]> embs = embeddings(java.util.Arrays.asList(entry.getValue()));
                if (embs != null && !embs.isEmpty()) result.put(entry.getKey(), embs);
            }
            if (!result.isEmpty()) {
                intentTemplateEmbeddings = result;
                log.info("Intent template embeddings initialized: {} scenes", result.size());
            }
        } catch (Exception e) {
            log.warn("Failed to init intent template embeddings: {}", e.getMessage());
        }
    }

    private String classifyIntentByEmbedding(String query) {
        if (intentTemplateEmbeddings == null) initIntentTemplateEmbeddings();
        if (intentTemplateEmbeddings == null) return null;
        float[] queryEmb = embedding(query);
        if (queryEmb == null || queryEmb.length == 0) return null;
        String bestScene = "chat";
        double bestScore = -1;
        for (Map.Entry<String, List<float[]>> entry : intentTemplateEmbeddings.entrySet()) {
            for (float[] templateEmb : entry.getValue()) {
                double score = cosineSim(queryEmb, templateEmb);
                if (score > bestScore) { bestScore = score; bestScene = entry.getKey(); }
            }
        }
        return bestScene;
    }

    public RoutedConfig routeModel(List<Map<String, Object>> messages) {
        String lastUserMessage = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).get("role"))) {
                lastUserMessage = (String) messages.get(i).get("content");
                break;
            }
        }
        if (lastUserMessage == null) return new RoutedConfig(currentBaseUrl, currentApiKey, currentModel);
        String scene = "chat";
        if (lastUserMessage.contains("[图片:")) {
            scene = "vision";
        } else {
            String embScene = classifyIntentByEmbedding(lastUserMessage);
            if (embScene != null) {
                scene = embScene;
            } else {
                String lower = lastUserMessage.toLowerCase();
                String[] reasonerKeywords = {
                    "分析", "计算", "比较", "推荐", "统计", "趋势", "为什么", "怎么算",
                    "哪种好", "区别", "优缺点", "建议", "规划", "预测", "评估", "对比"
                };
                for (String kw : reasonerKeywords) {
                    if (lower.contains(kw)) { scene = "reasoning"; break; }
                }
            }
        }
        String[] routed = routeByScene(scene);
        if (routed != null) {
            log.info("Model routing via channel: scene={} -> model={}", scene, routed[2]);
            return new RoutedConfig(routed[0], routed[1], routed[2]);
        }
        if ("vision".equals(scene) && currentVisionModel != null && !currentVisionModel.isEmpty()) {
            log.info("Model routing: vision model: {}", currentVisionModel);
            return new RoutedConfig(currentBaseUrl, currentApiKey, currentVisionModel);
        }
        if ("reasoning".equals(scene) && currentRoutingEnabled && currentReasonerModel != null) {
            return new RoutedConfig(currentBaseUrl, currentApiKey, currentReasonerModel);
        }
        return new RoutedConfig(currentBaseUrl, currentApiKey, currentModel);
    }

    public String getModel() {
        return currentModel;
    }

    public String getEffectiveModel() {
        String[] routed = routeByScene("chat");
        if (routed != null && routed[2] != null && !routed[2].isEmpty()) {
            return routed[2];
        }
        return currentModel;
    }

    public void updateApiKey(String newKey) {
        if (newKey != null && !newKey.isEmpty()) {
            currentApiKey = newKey;
            try { stringRedisTemplate.opsForValue().set(REDIS_KEY_APIKEY, newKey); } catch (Exception ignored) {}
            log.info("DeepSeek API key updated and persisted");
        }
    }

    public void updateModel(String newModel) {
        if (newModel != null && !newModel.isEmpty()) {
            currentModel = newModel;
            try { stringRedisTemplate.opsForValue().set(REDIS_KEY_MODEL, newModel); } catch (Exception ignored) {}
            log.info("DeepSeek model updated and persisted: {}", newModel);
        }
    }

    public void updateBaseUrl(String newUrl) {
        if (newUrl != null && !newUrl.isEmpty()) {
            currentBaseUrl = newUrl.replaceAll("/+$", "");
            try { stringRedisTemplate.opsForValue().set(REDIS_KEY_BASEURL, currentBaseUrl); } catch (Exception ignored) {}
            log.info("DeepSeek base URL updated and persisted: {}", currentBaseUrl);
        }
    }

    public void updateEmbeddingApiKey(String newKey) {
        currentEmbApiKey = newKey != null ? newKey : "";
        try { stringRedisTemplate.opsForValue().set(REDIS_KEY_EMB_APIKEY, currentEmbApiKey); } catch (Exception ignored) {}
        clearEmbeddingAvailableCache();
        log.info("Embedding API key updated and persisted");
    }

    public void updateEmbeddingBaseUrl(String newUrl) {
        currentEmbBaseUrl = newUrl != null ? newUrl.replaceAll("/+$", "") : "";
        try { stringRedisTemplate.opsForValue().set(REDIS_KEY_EMB_BASEURL, currentEmbBaseUrl); } catch (Exception ignored) {}
        clearEmbeddingAvailableCache();
        log.info("Embedding base URL updated and persisted: {}", currentEmbBaseUrl);
    }

    public void updateEmbeddingModel(String newModel) {
        if (newModel != null && !newModel.isEmpty()) {
            currentEmbModel = newModel;
            try { stringRedisTemplate.opsForValue().set(REDIS_KEY_EMB_MODEL, newModel); } catch (Exception ignored) {}
            clearEmbeddingAvailableCache();
            log.info("Embedding model updated and persisted: {}", newModel);
        }
    }

    public void updateRoutingEnabled(boolean enabled) {
        currentRoutingEnabled = enabled;
        try { stringRedisTemplate.opsForValue().set(REDIS_KEY_ROUTING_ENABLED, String.valueOf(enabled)); } catch (Exception ignored) {}
        log.info("Routing enabled updated and persisted: {}", enabled);
    }

    public void updateReasonerModel(String newModel) {
        if (newModel != null && !newModel.isEmpty()) {
            currentReasonerModel = newModel;
            try { stringRedisTemplate.opsForValue().set(REDIS_KEY_ROUTING_REASONER, newModel); } catch (Exception ignored) {}
            log.info("Reasoner model updated and persisted: {}", newModel);
        }
    }

    public void updateVisionModel(String newModel) {
        currentVisionModel = newModel != null ? newModel : "";
        try { stringRedisTemplate.opsForValue().set(REDIS_KEY_VISION_MODEL, currentVisionModel); } catch (Exception ignored) {}
        log.info("Vision model updated and persisted: {}", currentVisionModel);
    }

    public String getCurrentEmbApiKeyMasked() {
        if (currentEmbApiKey == null || currentEmbApiKey.isEmpty()) return "";
        if (currentEmbApiKey.length() < 8) return "****";
        return currentEmbApiKey.substring(0, 4) + "****" + currentEmbApiKey.substring(currentEmbApiKey.length() - 4);
    }

    public String getCurrentEmbBaseUrl() { return currentEmbBaseUrl != null ? currentEmbBaseUrl : ""; }
    public String getCurrentEmbModel() { return currentEmbModel; }
    public boolean isCurrentRoutingEnabled() { return currentRoutingEnabled; }
    public String getCurrentReasonerModel() { return currentReasonerModel; }
    public String getCurrentVisionModel() { return currentVisionModel != null ? currentVisionModel : ""; }

    public String getCurrentApiKeyMasked() {
        if (currentApiKey == null || currentApiKey.length() < 8) return "";
        return currentApiKey.substring(0, 4) + "****" + currentApiKey.substring(currentApiKey.length() - 4);
    }

    public String getCurrentBaseUrl() {
        return currentBaseUrl;
    }

    public boolean isCircuitBreakerOpen() {
        return cbOpen;
    }

    private final java.util.List<java.util.function.Consumer<String>> cbStateListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addCbStateListener(java.util.function.Consumer<String> listener) {
        cbStateListeners.add(listener);
    }

    private void notifyCbStateChange(String state) {
        for (java.util.function.Consumer<String> listener : cbStateListeners) {
            try { listener.accept(state); } catch (Exception ignored) {}
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 5000)
    public void scheduledCircuitBreakerCheck() {
        if (!cbOpen) return;
        long elapsed = System.currentTimeMillis() - cbOpenTime;
        if (elapsed < CB_RECOVERY_TIMEOUT_MS) return;
        log.info("Circuit breaker recovery timeout, probing API...");
        boolean apiOk = checkApiHealth();
        if (apiOk) {
            cbRecordSuccess();
            log.info("Circuit breaker auto-recovered via scheduled probe");
        } else {
            cbOpenTime = System.currentTimeMillis();
            persistCbState();
            notifyCbStateChange("open");
            log.warn("Circuit breaker re-opened after scheduled probe failure");
        }
    }

    public boolean checkApiHealth() {
        String[] routed = routeByScene("chat");
        if (routed != null) {
            return checkEndpointHealth(routed[0], routed[1]);
        }
        if (hasChannelWithChatCapability()) {
            log.warn("AI health check: channels configured but no enabled channel with chat model");
            return false;
        }
        return checkEndpointHealth(currentBaseUrl, currentApiKey);
    }

    public Map<String, Object> getApiStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        if (!isEnabled()) {
            status.put("status", "disabled");
            status.put("message", "AI服务未启用或API Key未配置");
            return status;
        }
        if (cbOpen) {
            long elapsed = System.currentTimeMillis() - cbOpenTime;
            if (elapsed > CB_RECOVERY_TIMEOUT_MS) {
                boolean apiOk = checkApiHealth();
                if (apiOk) {
                    cbRecordSuccess();
                    log.info("Circuit breaker auto-recovered via status probe");
                } else {
                    cbOpenTime = System.currentTimeMillis();
                    int timeoutSec = (int)(CB_RECOVERY_TIMEOUT_MS / 1000);
                    status.put("status", "circuit_breaker");
                    status.put("message", "熔断保护中（" + (lastFailureReason != null ? lastFailureReason : "API异常") + "），" + timeoutSec + "秒后重试");
                    status.put("remainingSeconds", timeoutSec);
                    return status;
                }
            } else {
                long remaining = (CB_RECOVERY_TIMEOUT_MS - elapsed) / 1000;
                String reason = lastFailureReason != null ? "（" + lastFailureReason + "）" : "";
                status.put("status", "circuit_breaker");
                status.put("message", "熔断保护中" + reason + "，" + Math.max(0, remaining) + "秒后重试");
                status.put("remainingSeconds", Math.max(0, remaining));
                return status;
            }
        }
        if (cachedApiStatus != null && (System.currentTimeMillis() - apiStatusCacheTime) < API_STATUS_CACHE_TTL_MS) {
            return new LinkedHashMap<>(cachedApiStatus);
        }
        int fc = cbFailureCount.get();
        if (fc > 0) {
            String reason = lastFailureReason != null ? lastFailureReason + "，" : "";
            status.put("status", "degraded");
            status.put("message", "API异常：" + reason + "连续" + fc + "次失败，" + (CB_FAILURE_THRESHOLD - fc) + "次后熔断");
            return status;
        }
        String[] routed = routeByScene("chat");
        String url = routed != null ? routed[0] : currentBaseUrl;
        String key = routed != null ? routed[1] : currentApiKey;
        if (url == null || key == null || key.isEmpty()) {
            status.put("status", "no_channel");
            status.put("message", "无可用渠道配置");
            return status;
        }
        int code = probeApiHealth(url, key);
        if (code >= 200 && code < 300) {
            status.put("status", "available");
            status.put("message", "API可用");
        } else if (code == 402) {
            status.put("status", "insufficient_balance");
            status.put("message", "API余额不足，请充值");
        } else if (code == 401) {
            status.put("status", "unauthorized");
            status.put("message", "API Key无效或已过期");
        } else if (code == 429) {
            status.put("status", "rate_limited");
            status.put("message", "API调用频率超限");
        } else if (code < 0) {
            status.put("status", "timeout");
            status.put("message", "API连接超时或网络异常");
        } else {
            status.put("status", "error");
            status.put("message", "API异常: HTTP " + code);
        }
        cachedApiStatus = new LinkedHashMap<>(status);
        apiStatusCacheTime = System.currentTimeMillis();
        return status;
    }

    private int probeApiHealth(String url, String key) {
        try {
            String model = getEffectiveModel();
            String body = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":1,\"stream\":false}";
            HttpResponse response = HttpRequest.post(url + "/chat/completions")
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .timeout(10000)
                    .body(body)
                    .execute();
            return response.getStatus();
        } catch (Exception e) {
            log.warn("API probe error: {}", e.getMessage());
            return -1;
        }
    }

    private boolean checkEndpointHealth(String url, String key) {
        int code = probeApiHealth(url, key);
        if (code >= 200 && code < 300) {
            return true;
        }
        log.warn("AI health check (POST /chat/completions) failed: HTTP {}", code);
        return false;
    }

    private boolean hasChannelWithChatCapability() {
        try {
            JSONArray channels = JSONUtil.parseArray(getChannelsJsonRaw());
            JSONArray models = JSONUtil.parseArray(getModelsJson());
            boolean hasChatModel = false;
            for (int i = 0; i < models.size(); i++) {
                JSONObject m = models.getJSONObject(i);
                JSONArray caps = m.getJSONArray("caps");
                if (caps != null) {
                    for (int j = 0; j < caps.size(); j++) {
                        if ("chat".equals(caps.getStr(j))) { hasChatModel = true; break; }
                    }
                }
                if (hasChatModel) break;
            }
            return !channels.isEmpty() && hasChatModel;
        } catch (Exception e) {
            return false;
        }
    }

    private void addThinkingParams(JSONObject payload, String model, boolean enable) {
        if (!enable) {
            payload.set("enable_thinking", false);
            return;
        }
        if (model == null) return;
        String lower = model.toLowerCase();
        if (lower.contains("reasoner") || lower.contains("thinking") || lower.contains("qwq")) {
            payload.set("enable_thinking", true);
        } else if (lower.contains("deepseek-v4") || lower.contains("glm-5")) {
            payload.set("enable_thinking", true);
            payload.set("reasoning_effort", "medium");
        } else if (lower.contains("deepseek-v3") || lower.contains("deepseek")) {
            payload.set("enable_thinking", true);
        } else if (lower.contains("qwen3") || lower.contains("qwen/qwen3")) {
            payload.set("enable_thinking", true);
        } else if (lower.contains("glm")) {
            payload.set("enable_thinking", true);
        } else if (lower.contains("gemini")) {
            payload.set("enable_thinking", true);
        }
    }

    public CompletableFuture<String> chatAsync(List<Map<String, Object>> messages) {
        return CompletableFuture.supplyAsync(() -> chat(messages), aiTaskExecutor);
    }

    public CompletableFuture<Void> chatStream(
            List<Map<String, Object>> messages, Consumer<String> onToken, Consumer<String> onReasoning, Consumer<Void> onDone, Consumer<Throwable> onError) {
        return chatStream(messages, onToken, onReasoning, onDone, onError, null);
    }

    public CompletableFuture<Void> chatStream(
            List<Map<String, Object>> messages, Consumer<String> onToken, Consumer<String> onReasoning, Consumer<Void> onDone, Consumer<Throwable> onError, String scene) {
        if (!isEnabled()) {
            onError.accept(new IllegalStateException("AI service disabled or api key not configured"));
            onDone.accept(null);
            return CompletableFuture.completedFuture(null);
        }
        if (!cbAllowRequest()) {
            log.warn("Circuit breaker open, failing fast for stream request");
            onError.accept(new RuntimeException("AI服务暂时不可用（熔断保护中），请稍后再试"));
            onDone.accept(null);
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            requestCounter.increment();
            incrRedisStat(REDIS_KEY_STAT_API_CALLS, totalApiCalls);
            HttpResponse response = null;
            try {
                if (!concurrencyLimit.tryAcquire(streamTimeoutMs, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("AI concurrent request limit reached");
                }
                JSONObject payload = new JSONObject();
                RoutedConfig routed;
                if (scene != null && !scene.isEmpty()) {
                    String[] routedArr = routeByScene(scene);
                    if (routedArr != null) {
                        routed = new RoutedConfig(routedArr[0], routedArr[1], routedArr[2]);
                        log.info("chatStream routing via scene: scene={} -> model={}", scene, routedArr[2]);
                    } else {
                        routed = routeModel(messages);
                        log.info("chatStream scene routing failed, fallback to routeModel: model={}", routed.model);
                    }
                } else {
                    routed = routeModel(messages);
                    log.info("chatStream routing via routeModel: model={}", routed.model);
                }
                payload.set("model", routed.model);
                List<Map<String, Object>> messagesToSend = convertMessagesForVision(messages, routed.model);
                payload.set("messages", JSONUtil.parseArray(JSONUtil.toJsonStr(messagesToSend)));
                payload.set("stream", true);
                payload.set("stream_options", JSONUtil.parseObj("{\"include_usage\": true}"));
                payload.set("temperature", 0.3);
                payload.set("max_tokens", maxTokens);
                addThinkingParams(payload, routed.model, true);

                response = HttpRequest.post(routed.baseUrl + "/chat/completions")
                        .header("Authorization", "Bearer " + routed.apiKey)
                        .header("Content-Type", "application/json")
                        .body(payload.toString())
                        .timeout(streamTimeoutMs)
                        .execute();

                int code = response.getStatus();
                if (code < 200 || code >= 300) {
                    errorCounter.increment();
                    cbRecordFailure(extractFailureReason(code, response.body()));
                    throw new RuntimeException("DeepSeek API error: " + code + " " + response.body());
                }
                try (InputStream is = response.bodyStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JSONObject obj = JSONUtil.parseObj(data);
                            JSONObject usage = obj.getJSONObject("usage");
                            if (usage != null) {
                                Integer t = usage.getInt("total_tokens");
                                if (t != null && t > 0) incrRedisStatBy(REDIS_KEY_STAT_TOKENS, totalTokens, t);
                            }
                            JSONArray choices = obj.getJSONArray("choices");
                            if (choices == null ||(choices.isEmpty())) continue;
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject delta = choice.getJSONObject("delta");
                            if (delta == null) continue;
                            String reasoningToken = delta.getStr("reasoning_content");
                            if (reasoningToken != null && !reasoningToken.isEmpty() && onReasoning != null) {
                                onReasoning.accept(reasoningToken);
                            }
                            String token = delta.getStr("content");
                            if (token != null && !token.isEmpty()) {
                                onToken.accept(token);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse DeepSeek stream line: {}", data, e);
                        }
                    }
                }
                cbRecordSuccess();
                incrRedisStat(REDIS_KEY_STAT_SUCCESS, successApiCalls);
                onDone.accept(null);
                return null;
            } catch (Exception e) {
                errorCounter.increment();
                incrRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
                cbRecordFailure(extractExceptionReason(e));
                onError.accept(e);
                return null;
            } finally {
                recordLatency(sample.stop(latencyTimer));
                concurrencyLimit.release();
                if (response != null) {
                    try { response.close(); } catch (Exception ignored) {}
                }
            }
        }, aiTaskExecutor);
    }

    public String chat(List<Map<String, Object>> messages) {
        if (!isEnabled()) {
            return FALLBACK_ANSWERS.get("faq");
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            String result = doChat(messages);
            if (!FALLBACK_ANSWERS.get("faq").equals(result)) return result;
            if (attempt == 0) log.info("Retrying DeepSeek chat after failure");
        }
        if (fallbackApiKey != null && !fallbackApiKey.isEmpty()) {
            String fbUrl = fallbackBaseUrl.isEmpty() ? currentBaseUrl : fallbackBaseUrl;
            String fbModel = fallbackModel.isEmpty() ? currentModel : fallbackModel;
            log.info("Trying fallback model: {}", fbModel);
            String result = doChatWithConfig(messages, fallbackApiKey, fbUrl, fbModel);
            if (!FALLBACK_ANSWERS.get("faq").equals(result)) return result;
        }
        return FALLBACK_ANSWERS.get("faq");
    }

    private String doChat(List<Map<String, Object>> messages) {
        RoutedConfig routed = routeModel(messages);
        return doChatWithConfig(messages, routed.apiKey, routed.baseUrl, routed.model);
    }

    private String doChatWithConfig(List<Map<String, Object>> messages, String chatKey, String chatUrl, String chatModel) {
        if (!cbAllowRequest()) {
            log.warn("Circuit breaker open, failing fast for chat request");
            return FALLBACK_ANSWERS.get("faq");
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        requestCounter.increment();
        incrRedisStat(REDIS_KEY_STAT_API_CALLS, totalApiCalls);
        try {
            if (!concurrencyLimit.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                return FALLBACK_ANSWERS.get("faq");
            }
            JSONObject payload = new JSONObject();
            payload.set("model", chatModel);
            List<Map<String, Object>> messagesToSend = convertMessagesForVision(messages, chatModel);
            payload.set("messages", JSONUtil.parseArray(JSONUtil.toJsonStr(messagesToSend)));
            payload.set("stream", false);
            payload.set("temperature", 0.3);
            payload.set("max_tokens", maxTokens);
            addThinkingParams(payload, chatModel, false);

            HttpResponse response = HttpRequest.post(chatUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + chatKey)
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .timeout(timeoutMs)
                    .execute();
            int code = response.getStatus();
            if (code < 200 || code >= 300) {
                errorCounter.increment();
                incrRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
                cbRecordFailure(extractFailureReason(code, response.body()));
                log.error("DeepSeek API error: code={}, body={}", code, response.body());
                return FALLBACK_ANSWERS.get("faq");
            }
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONObject usage = body.getJSONObject("usage");
            if (usage != null) {
                Integer t = usage.getInt("total_tokens");
                if (t != null && t > 0) incrRedisStatBy(REDIS_KEY_STAT_TOKENS, totalTokens, t);
            }
            JSONArray choices = body.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                cbRecordFailure("API返回空数据");
                return FALLBACK_ANSWERS.get("faq");
            }
            cbRecordSuccess();
            incrRedisStat(REDIS_KEY_STAT_SUCCESS, successApiCalls);
            return choices.getJSONObject(0).getJSONObject("message").getStr("content");
        } catch (Exception e) {
            errorCounter.increment();
            incrRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
            cbRecordFailure(extractExceptionReason(e));
            log.error("DeepSeek API call failed", e);
            return FALLBACK_ANSWERS.get("faq");
        } finally {
            recordLatency(sample.stop(latencyTimer));
            concurrencyLimit.release();
        }
    }

    public Map<String, Object> chatWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        return chatWithToolsForScene(messages, tools, null);
    }

    public Map<String, Object> chatWithToolsForScene(List<Map<String, Object>> messages, List<Map<String, Object>> tools, String sceneOverride) {
        Map<String, Object> result = new HashMap<>();
        if (!isEnabled()) {
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            result.put("toolCalls", null);
            return result;
        }
        result = doChatWithToolsForScene(messages, tools, sceneOverride);
        String content = (String) result.get("content");
        if (content != null && !FALLBACK_ANSWERS.get("faq").equals(content)) {
            return result;
        }
        if (fallbackApiKey != null && !fallbackApiKey.isEmpty()) {
            String fbUrl = fallbackBaseUrl.isEmpty() ? currentBaseUrl : fallbackBaseUrl;
            String fbModel = fallbackModel.isEmpty() ? currentModel : fallbackModel;
            log.info("Trying fallback model (tools): {}", fbModel);
            result = doChatWithToolsConfig(messages, tools, fallbackApiKey, fbUrl, fbModel);
            content = (String) result.get("content");
            if (content != null && !FALLBACK_ANSWERS.get("faq").equals(content)) return result;
        }
        return result;
    }

    private Map<String, Object> doChatWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        return doChatWithToolsForScene(messages, tools, null);
    }

    private Map<String, Object> doChatWithToolsForScene(List<Map<String, Object>> messages, List<Map<String, Object>> tools, String sceneOverride) {
        RoutedConfig routed;
        if (sceneOverride != null && !sceneOverride.isEmpty()) {
            String[] routedArr = routeByScene(sceneOverride);
            if (routedArr != null) {
                routed = new RoutedConfig(routedArr[0], routedArr[1], routedArr[2]);
                log.info("Model routing via scene override: scene={} -> model={}", sceneOverride, routedArr[2]);
            } else {
                routed = routeModel(messages);
            }
        } else {
            routed = routeModel(messages);
        }
        return doChatWithToolsConfig(messages, tools, routed.apiKey, routed.baseUrl, routed.model);
    }

    public Map<String, Object> chatWithToolsStream(
            List<Map<String, Object>> messages, List<Map<String, Object>> tools, String sceneOverride,
            Consumer<String> onReasoning, Consumer<String> onContent, Runnable onReasoningEnd) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", null);
        result.put("reasoning_content", null);
        result.put("toolCalls", null);
        if (!isEnabled()) { result.put("content", FALLBACK_ANSWERS.get("faq")); return result; }

        RoutedConfig routed;
        if (sceneOverride != null && !sceneOverride.isEmpty()) {
            String[] routedArr = routeByScene(sceneOverride);
            if (routedArr != null) {
                routed = new RoutedConfig(routedArr[0], routedArr[1], routedArr[2]);
                log.info("chatWithToolsStream routing: scene={} -> model={}", sceneOverride, routedArr[2]);
            } else { routed = routeModel(messages); }
        } else { routed = routeModel(messages); }

        try {
            JSONObject payload = new JSONObject();
            payload.set("model", routed.model);
            payload.set("messages", JSONUtil.parseArray(JSONUtil.toJsonStr(convertMessagesForVision(messages, routed.model))));
            payload.set("stream", true);
            payload.set("stream_options", JSONUtil.parseObj("{\"include_usage\": true}"));
            payload.set("temperature", 0.3);
            payload.set("max_tokens", maxTokens);
            addThinkingParams(payload, routed.model, true);
            if (tools != null && !tools.isEmpty()) payload.set("tools", JSONUtil.parseArray(tools));

            java.net.URL apiUrl = new java.net.URL(routed.baseUrl + "/chat/completions");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + routed.apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(streamTimeoutMs);
            conn.setDoOutput(true);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.error("chatWithToolsStream API error: code={}", code);
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                conn.disconnect();
                return result;
            }

            StringBuilder contentBuilder = new StringBuilder();
            StringBuilder reasoningBuilder = new StringBuilder();
            Map<Integer, JSONObject> tcBuffers = new LinkedHashMap<>();
            final boolean[] reasoningEnded = {false};

            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JSONObject obj = JSONUtil.parseObj(data);
                        JSONArray choices = obj.getJSONArray("choices");
                        if (choices == null || choices.isEmpty()) continue;
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject delta = choice.getJSONObject("delta");
                        if (delta == null) continue;

                        String reasoningToken = delta.getStr("reasoning_content");
                        if (reasoningToken != null && !reasoningToken.isEmpty()) {
                            reasoningBuilder.append(reasoningToken);
                            if (onReasoning != null) onReasoning.accept(reasoningToken);
                        }

                        String contentToken = delta.getStr("content");
                        JSONArray tcPeek = delta.getJSONArray("tool_calls");
                        if ((contentToken != null && !contentToken.isEmpty()) || (tcPeek != null && !tcPeek.isEmpty())) {
                            if (reasoningBuilder.length() > 0 && !reasoningEnded[0]) {
                                reasoningEnded[0] = true;
                                if (onReasoningEnd != null) onReasoningEnd.run();
                            }
                        }
                        if (contentToken != null && !contentToken.isEmpty()) {
                            contentBuilder.append(contentToken);
                            if (onContent != null) onContent.accept(contentToken);
                        }

                        JSONArray deltaToolCalls = delta.getJSONArray("tool_calls");
                        if (deltaToolCalls != null) {
                            for (int ti = 0; ti < deltaToolCalls.size(); ti++) {
                                JSONObject tc = deltaToolCalls.getJSONObject(ti);
                                int idx = tc.getInt("index", 0);
                                JSONObject buf = tcBuffers.computeIfAbsent(idx, k -> {
                                    JSONObject b = new JSONObject();
                                    b.set("id", "");
                                    b.set("name", "");
                                    b.set("arguments", "");
                                    return b;
                                });
                                if (tc.getStr("id") != null && !tc.getStr("id").isEmpty()) buf.set("id", tc.getStr("id"));
                                JSONObject fn = tc.getJSONObject("function");
                                if (fn != null) {
                                    if (fn.getStr("name") != null && !fn.getStr("name").isEmpty()) buf.set("name", fn.getStr("name"));
                                    String argsFrag = fn.getStr("arguments");
                                    if (argsFrag != null) buf.set("arguments", buf.getStr("arguments") + argsFrag);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse chatWithToolsStream line: {}", data, e);
                    }
                }
            }

            result.put("content", contentBuilder.toString());
            result.put("reasoning_content", reasoningBuilder.toString());
            if (!tcBuffers.isEmpty()) {
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                for (JSONObject buf : tcBuffers.values()) {
                    Map<String, Object> tc = new LinkedHashMap<>();
                    tc.put("id", buf.getStr("id"));
                    Map<String, Object> fn = new LinkedHashMap<>();
                    fn.put("name", buf.getStr("name"));
                    fn.put("arguments", buf.getStr("arguments"));
                    tc.put("function", fn);
                    tc.put("type", "function");
                    toolCalls.add(tc);
                }
                result.put("toolCalls", toolCalls);
            }
            log.info("chatWithToolsStream done: contentLen={} reasoningLen={} toolCalls={}",
                contentBuilder.length(), reasoningBuilder.length(), tcBuffers.size());
            if (reasoningBuilder.length() > 0) {
                String rtail = reasoningBuilder.length() > 160 ? reasoningBuilder.substring(reasoningBuilder.length() - 160) : reasoningBuilder.toString();
                String chead = contentBuilder.length() > 160 ? contentBuilder.substring(0, 160) : contentBuilder.toString();
                log.info("chatWithToolsStream VERIFY: reasoningTail=[{}] contentHead=[{}]", rtail, chead);
            }
            conn.disconnect();
            return result;
        } catch (Exception e) {
            log.error("chatWithToolsStream failed", e);
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            return result;
        }
    }

    private boolean isVisionModel(String modelName) {
        if (modelName == null || modelName.isEmpty()) return false;
        try {
            JSONArray models = JSONUtil.parseArray(getModelsJson());
            for (int i = 0; i < models.size(); i++) {
                JSONObject m = models.getJSONObject(i);
                if (modelName.equals(m.getStr("model"))) {
                    JSONArray caps = m.getJSONArray("caps");
                    if (caps != null) {
                        for (int j = 0; j < caps.size(); j++) {
                            if ("vision".equals(caps.getStr(j))) return true;
                        }
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            log.warn("isVisionModel check failed: {}", e.getMessage());
        }
        return false;
    }

    private List<Map<String, Object>> convertMessagesForVision(List<Map<String, Object>> messages, String targetModel) {
        boolean hasImage = false;
        for (Map<String, Object> msg : messages) {
            if ("user".equals(msg.get("role"))) {
                Object content = msg.get("content");
                if (content instanceof String && ((String) content).contains("[图片:")) {
                    hasImage = true;
                    break;
                }
            }
        }
        if (!hasImage) return messages;

        boolean modelSupportsVision = isVisionModel(targetModel);
        Pattern imgPattern = Pattern.compile("\\[图片:\\s*([^\\]]+)\\]\\(([^)]+)\\)");

        if (!modelSupportsVision) {
            log.info("Target model {} is not VLM, stripping image markers from messages", targetModel);
            List<Map<String, Object>> stripped = new ArrayList<>();
            for (Map<String, Object> msg : messages) {
                if ("user".equals(msg.get("role"))) {
                    Object contentObj = msg.get("content");
                    if (contentObj instanceof String) {
                        String content = (String) contentObj;
                        if (content.contains("[图片:")) {
                            String cleaned = imgPattern.matcher(content).replaceAll("[图片]");
                            Map<String, Object> newMsg = new HashMap<>(msg);
                            newMsg.put("content", cleaned);
                            stripped.add(newMsg);
                            continue;
                        }
                    }
                }
                stripped.add(msg);
            }
            return stripped;
        }

        log.info("Converting messages to multimodal format for vision model: {}", targetModel);
        List<Map<String, Object>> converted = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            if ("user".equals(msg.get("role"))) {
                Object contentObj = msg.get("content");
                if (contentObj instanceof String) {
                    String content = (String) contentObj;
                    if (content.contains("[图片:")) {
                        List<Object> multimodalContent = new ArrayList<>();
                        Matcher matcher = imgPattern.matcher(content);
                        StringBuffer textPart = new StringBuffer();
                        while (matcher.find()) {
                            String imageUrl = matcher.group(2);
                            matcher.appendReplacement(textPart, "");
                            String dataUrl = imageToDataUrl(imageUrl);
                            if (dataUrl != null) {
                                Map<String, Object> imagePart = new HashMap<>();
                                imagePart.put("type", "image_url");
                                Map<String, String> urlMap = new HashMap<>();
                                urlMap.put("url", dataUrl);
                                imagePart.put("image_url", urlMap);
                                multimodalContent.add(imagePart);
                                log.info("Image converted to base64: {} ({} chars)", imageUrl, dataUrl.length());
                            }
                        }
                        matcher.appendTail(textPart);
                        String remainingText = textPart.toString().trim();
                        if (!remainingText.isEmpty()) {
                            Map<String, Object> textPartMap = new HashMap<>();
                            textPartMap.put("type", "text");
                            textPartMap.put("text", remainingText);
                            multimodalContent.add(textPartMap);
                        }
                        Map<String, Object> newMsg = new HashMap<>(msg);
                        newMsg.put("content", multimodalContent);
                        converted.add(newMsg);
                        continue;
                    }
                }
            }
            converted.add(msg);
        }
        return converted;
    }

    private String imageToDataUrl(String urlPath) {
        try {
            String filePath = urlPath.replace(urlPrefix, uploadBasePath);
            File file = new File(filePath);
            String canonical = file.getCanonicalPath();
            String baseCanonical = new File(uploadBasePath).getCanonicalPath();
            if (!canonical.equals(baseCanonical) && !canonical.startsWith(baseCanonical + File.separator)) {
                log.warn("Blocked path traversal attempt: {}", urlPath);
                return null;
            }
            String lower = canonical.toLowerCase();
            if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg")
                    && !lower.endsWith(".gif") && !lower.endsWith(".webp")) {
                log.warn("Blocked non-image file access: {}", canonical);
                return null;
            }
            if (!file.exists()) {
                log.warn("Image file not found: {}", filePath);
                return null;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mimeType = "image/jpeg";
            else if (lower.endsWith(".gif")) mimeType = "image/gif";
            else if (lower.endsWith(".webp")) mimeType = "image/webp";
            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.warn("Failed to convert image to base64: {}", urlPath, e);
            return null;
        }
    }

    private Map<String, Object> doChatWithToolsConfig(List<Map<String, Object>> messages, List<Map<String, Object>> tools, String chatKey, String chatUrl, String chatModel) {
        Map<String, Object> result = new HashMap<>();
        if (!isEnabled()) {
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            result.put("toolCalls", null);
            return result;
        }
        if (!cbAllowRequest()) {
            log.warn("Circuit breaker open, failing fast for chatWithTools request");
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            result.put("toolCalls", null);
            return result;
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        requestCounter.increment();
        incrRedisStat(REDIS_KEY_STAT_API_CALLS, totalApiCalls);
        try {
            if (!concurrencyLimit.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                result.put("toolCalls", null);
                return result;
            }
            JSONObject payload = new JSONObject();
            payload.set("model", chatModel);
            List<Map<String, Object>> messagesToSend = convertMessagesForVision(messages, chatModel);
            payload.set("messages", JSONUtil.parseArray(JSONUtil.toJsonStr(messagesToSend)));
            payload.set("stream", false);
            payload.set("temperature", 0.3);
            payload.set("max_tokens", maxTokens);
            addThinkingParams(payload, chatModel, false);
            if (tools != null && !tools.isEmpty()) {
                payload.set("tools", JSONUtil.parseArray(tools));
            }

            HttpResponse response = HttpRequest.post(chatUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + chatKey)
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .timeout(timeoutMs)
                    .execute();
            int code = response.getStatus();
            if (code < 200 || code >= 300) {
                errorCounter.increment();
                incrRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
                cbRecordFailure(extractFailureReason(code, response.body()));
                log.error("DeepSeek API error: code={}, body={}", code, response.body());
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                result.put("toolCalls", null);
                return result;
            }
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONObject usage = body.getJSONObject("usage");
            if (usage != null) {
                Integer t = usage.getInt("total_tokens");
                if (t != null && t > 0) incrRedisStatBy(REDIS_KEY_STAT_TOKENS, totalTokens, t);
            }
            JSONArray choices = body.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                cbRecordFailure("API返回空数据");
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                result.put("toolCalls", null);
                return result;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            result.put("content", message.getStr("content"));
            result.put("reasoning_content", message.getStr("reasoning_content"));
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            result.put("toolCalls", toolCalls != null && !toolCalls.isEmpty() ? JSONUtil.toList(toolCalls, Map.class) : null);
            cbRecordSuccess();
            incrRedisStat(REDIS_KEY_STAT_SUCCESS, successApiCalls);
            return result;
        } catch (Exception e) {
            errorCounter.increment();
            incrRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
            cbRecordFailure(extractExceptionReason(e));
            log.error("DeepSeek chatWithTools failed", e);
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            result.put("toolCalls", null);
            return result;
        } finally {
            recordLatency(sample.stop(latencyTimer));
            concurrencyLimit.release();
        }
    }

    public List<float[]> embeddings(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        if (texts == null || texts.isEmpty()) return result;
        String embKey = null, embUrl = null, embModel = null;
        String[] routed = routeByScene("embedding");
        if (routed != null) {
            embUrl = routed[0];
            embKey = routed[1];
            embModel = routed[2];
        }
        if (embKey == null || embKey.isEmpty()) {
            embKey = (currentEmbApiKey != null && !currentEmbApiKey.isEmpty()) ? currentEmbApiKey : currentApiKey;
            embUrl = (currentEmbBaseUrl != null && !currentEmbBaseUrl.isEmpty()) ? currentEmbBaseUrl : currentBaseUrl;
            embModel = (currentEmbModel != null && !currentEmbModel.isEmpty()) ? currentEmbModel : null;
        }
        if (embKey == null || embKey.isEmpty() || embModel == null) return result;
        if (!cbAllowRequest()) {
            log.warn("Circuit breaker open, failing fast for embedding request");
            return result;
        }
        try {
            if (!concurrencyLimit.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                log.warn("Embedding request concurrency limit reached");
                return result;
            }
            JSONObject payload = new JSONObject();
            payload.set("model", embModel);
            payload.set("input", JSONUtil.parseArray(texts));
            HttpResponse response = HttpRequest.post(embUrl + "/embeddings")
                    .header("Authorization", "Bearer " + embKey)
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .timeout(timeoutMs)
                    .execute();
            int code = response.getStatus();
            if (code < 200 || code >= 300) {
                log.warn("Embedding API error: {} {}", code, response.body());
                cbRecordFailure(extractFailureReason(code, response.body()));
                return result;
            }
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray data = body.getJSONArray("data");
            if (data == null) {
                cbRecordFailure("API返回空数据");
                return result;
            }
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                JSONArray embedding = item.getJSONArray("embedding");
                float[] vec = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) {
                    vec[j] = embedding.getFloat(j);
                }
                result.add(vec);
            }
            cbRecordSuccess();
            return result;
        } catch (Exception e) {
            log.warn("Embedding API call failed: {}", e.getMessage());
            cbRecordFailure(extractExceptionReason(e));
            return result;
        } finally {
            concurrencyLimit.release();
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<String, CachedEmbedding> queryEmbCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long QUERY_EMB_CACHE_TTL_MS = 60_000L;

    private static class CachedEmbedding {
        final float[] vec;
        final long ts;
        CachedEmbedding(float[] v, long t) { vec = v; ts = t; }
    }

    public float[] embedding(String text) {
        if (text == null || text.isEmpty()) return null;
        String key = text.length() + ":" + text.hashCode();
        CachedEmbedding ce = queryEmbCache.get(key);
        if (ce != null && System.currentTimeMillis() - ce.ts < QUERY_EMB_CACHE_TTL_MS) {
            return ce.vec;
        }
        List<float[]> results = embeddings(java.util.Collections.singletonList(text));
        float[] vec = results.isEmpty() ? null : results.get(0);
        if (vec != null) {
            if (queryEmbCache.size() > 1000) queryEmbCache.clear();
            queryEmbCache.put(key, new CachedEmbedding(vec, System.currentTimeMillis()));
        }
        return vec;
    }

    private volatile long embeddingAvailableCacheTime = 0;
    private volatile boolean embeddingAvailableCachedResult = false;
    private static final long EMBEDDING_CACHE_TTL_MS = 5 * 60 * 1000;

    public void clearEmbeddingAvailableCache() {
        embeddingAvailableCacheTime = 0;
        embeddingAvailableCachedResult = false;
    }

    public boolean isEmbeddingAvailable() {
        long now = System.currentTimeMillis();
        if (now - embeddingAvailableCacheTime < EMBEDDING_CACHE_TTL_MS) {
            return embeddingAvailableCachedResult;
        }
        boolean result;
        try {
            float[] test = embedding("测试");
            result = test != null && test.length > 0;
        } catch (Exception e) {
            result = false;
        }
        embeddingAvailableCachedResult = result;
        embeddingAvailableCacheTime = now;
        return result;
    }

    // ==================== 渠道+模型注册管理 ====================
    private static final String REDIS_KEY_CHANNELS = "ai:channels";
    private static final String REDIS_KEY_MODELS = "ai:models";

    public String getChannelsJson() {
        try {
            String json = stringRedisTemplate.opsForValue().get(REDIS_KEY_CHANNELS);
            if (json == null) return "[]";
            JSONArray arr = JSONUtil.parseArray(json);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject ch = arr.getJSONObject(i);
                String key = ch.getStr("apiKey");
                if (key != null && key.length() > 8) {
                    ch.set("apiKey", key.substring(0, 4) + "****" + key.substring(key.length() - 4));
                }
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private String getChannelsJsonRaw() {
        try {
            String json = stringRedisTemplate.opsForValue().get(REDIS_KEY_CHANNELS);
            return json != null ? json : "[]";
        } catch (Exception e) {
            return "[]";
        }
    }

    public void saveChannelsJson(String json) {
        try {
            JSONArray incoming = JSONUtil.parseArray(json);
            String existingJson = stringRedisTemplate.opsForValue().get(REDIS_KEY_CHANNELS);
            JSONArray existing = existingJson != null ? JSONUtil.parseArray(existingJson) : new JSONArray();
            for (int i = 0; i < incoming.size(); i++) {
                JSONObject ch = incoming.getJSONObject(i);
                String key = ch.getStr("apiKey");
                if (key != null && key.contains("****")) {
                    String chId = ch.getStr("id");
                    for (int j = 0; j < existing.size(); j++) {
                        JSONObject ex = existing.getJSONObject(j);
                        if (chId != null && chId.equals(ex.getStr("id"))) {
                            ch.set("apiKey", ex.getStr("apiKey"));
                            break;
                        }
                    }
                }
            }
            stringRedisTemplate.opsForValue().set(REDIS_KEY_CHANNELS, incoming.toString());
            clearEmbeddingAvailableCache();
            reloadConfig();
            log.info("AI channels updated");
        } catch (Exception e) {
            log.warn("Failed to save channels: {}", e.getMessage());
        }
    }

    public String getModelsJson() {
        try {
            String json = stringRedisTemplate.opsForValue().get(REDIS_KEY_MODELS);
            return json != null ? json : "[]";
        } catch (Exception e) {
            return "[]";
        }
    }

    public void saveModelsJson(String json) {
        try {
            stringRedisTemplate.opsForValue().set(REDIS_KEY_MODELS, json);
            clearEmbeddingAvailableCache();
            reloadConfig();
            log.info("AI models updated");
        } catch (Exception e) {
            log.warn("Failed to save models: {}", e.getMessage());
        }
    }

    /**
     * 根据场景路由到渠道+模型
     * @param scene chat/reasoning/vision/embedding
     * @return [baseUrl, apiKey, model] 或 null
     */
    public String[] routeByScene(String scene) {
        try {
            JSONArray channels = JSONUtil.parseArray(getChannelsJsonRaw());
            JSONArray models = JSONUtil.parseArray(getModelsJson());
            List<JSONObject> candidates = new ArrayList<>();
            for (int i = 0; i < models.size(); i++) {
                JSONObject m = models.getJSONObject(i);
                JSONArray caps = m.getJSONArray("caps");
                boolean hasCap = false;
                if (caps != null) {
                    for (int j = 0; j < caps.size(); j++) {
                        if (scene.equals(caps.getStr(j))) { hasCap = true; break; }
                    }
                }
                if (!hasCap) continue;
                String channelId = m.getStr("channelId");
                for (int j = 0; j < channels.size(); j++) {
                    JSONObject ch = channels.getJSONObject(j);
                    if (channelId.equals(ch.getStr("id")) && ch.getBool("enabled", false)) {
                        JSONObject candidate = new JSONObject();
                        candidate.set("baseUrl", ch.getStr("baseUrl"));
                        candidate.set("apiKey", ch.getStr("apiKey"));
                        candidate.set("model", m.getStr("model"));
                        candidate.set("priority", ch.getInt("priority", 99));
                        candidates.add(candidate);
                        break;
                    }
                }
            }
            if (candidates.isEmpty()) return null;
            candidates.sort(Comparator.comparingInt(c -> c.getInt("priority", 99)));
            JSONObject best = candidates.get(0);
            return new String[]{ best.getStr("baseUrl"), best.getStr("apiKey"), best.getStr("model") };
        } catch (Exception e) {
            log.warn("routeByScene failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean isVisionAvailable() {
        return routeByScene("vision") != null;
    }

}