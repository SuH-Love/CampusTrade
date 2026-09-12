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
    private static final String REDIS_KEY_STAT_TOKENS = "ai:stats:total_tokens";


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

    private volatile int cbFailureCount = 0;
    private volatile long cbOpenTime = 0;
    private volatile boolean cbOpen = false;
    private static final int CB_FAILURE_THRESHOLD = 5;
    private static final long CB_RECOVERY_TIMEOUT_MS = 30000;

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

    private boolean cbAllowRequest() {
        if (!cbOpen) return true;
        if (System.currentTimeMillis() - cbOpenTime > CB_RECOVERY_TIMEOUT_MS) {
            log.info("Circuit breaker entering half-open state");
            return true;
        }
        return false;
    }

    private void cbRecordSuccess() {
        if (cbFailureCount > 0 || cbOpen) {
            log.info("Circuit breaker closed (recovered)");
        }
        cbFailureCount = 0;
        cbOpen = false;
    }

    private void cbRecordFailure() {
        cbFailureCount++;
        if (cbFailureCount >= CB_FAILURE_THRESHOLD && !cbOpen) {
            cbOpen = true;
            cbOpenTime = System.currentTimeMillis();
            log.warn("Circuit breaker opened after {} consecutive failures", cbFailureCount);
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
            stats.put("avgLatencyMs", Math.round(latencyTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS) * 10) / 10.0);
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

    private boolean checkEndpointHealth(String url, String key) {
        try {
            HttpResponse response = HttpRequest.get(url + "/models")
                    .header("Authorization", "Bearer " + key)
                    .timeout(10000)
                    .execute();
            int code = response.getStatus();
            if (code >= 200 && code < 300) {
                return true;
            }
            log.warn("AI health check (GET /models) failed: {} {}", code, response.body());
            return false;
        } catch (Exception e) {
            log.warn("AI health check (GET /models) error: {}", e.getMessage());
            return false;
        }
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

    public CompletableFuture<String> chatAsync(List<Map<String, Object>> messages) {
        return CompletableFuture.supplyAsync(() -> chat(messages), aiTaskExecutor);
    }

    public CompletableFuture<Void> chatStream(
            List<Map<String, Object>> messages, Consumer<String> onToken, Consumer<Void> onDone, Consumer<Throwable> onError) {
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
                RoutedConfig routed = routeModel(messages);
                payload.set("model", routed.model);
                List<Map<String, Object>> messagesToSend = convertMessagesForVision(messages, routed.model);
                payload.set("messages", JSONUtil.parseArray(JSONUtil.toJsonStr(messagesToSend)));
                payload.set("stream", true);
                payload.set("stream_options", JSONUtil.parseObj("{\"include_usage\": true}"));
                payload.set("temperature", 0.3);
                payload.set("max_tokens", maxTokens);

                response = HttpRequest.post(routed.baseUrl + "/chat/completions")
                        .header("Authorization", "Bearer " + routed.apiKey)
                        .header("Content-Type", "application/json")
                        .body(payload.toString())
                        .timeout(streamTimeoutMs)
                        .execute();

                int code = response.getStatus();
                if (code < 200 || code >= 300) {
                    errorCounter.increment();
                    cbRecordFailure();
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
                cbRecordFailure();
                onError.accept(e);
                return null;
            } finally {
                sample.stop(latencyTimer);
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
                cbRecordFailure();
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
                cbRecordFailure();
                return FALLBACK_ANSWERS.get("faq");
            }
            cbRecordSuccess();
            incrRedisStat(REDIS_KEY_STAT_SUCCESS, successApiCalls);
            return choices.getJSONObject(0).getJSONObject("message").getStr("content");
        } catch (Exception e) {
            errorCounter.increment();
            incrRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
            cbRecordFailure();
            log.error("DeepSeek API call failed", e);
            return FALLBACK_ANSWERS.get("faq");
        } finally {
            sample.stop(latencyTimer);
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
            if (!file.exists()) {
                log.warn("Image file not found: {}", filePath);
                return null;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = "image/png";
            if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) mimeType = "image/jpeg";
            else if (filePath.endsWith(".gif")) mimeType = "image/gif";
            else if (filePath.endsWith(".webp")) mimeType = "image/webp";
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
                cbRecordFailure();
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
                cbRecordFailure();
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                result.put("toolCalls", null);
                return result;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            result.put("content", message.getStr("content"));
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            result.put("toolCalls", toolCalls != null && !toolCalls.isEmpty() ? JSONUtil.toList(toolCalls, Map.class) : null);
            cbRecordSuccess();
            incrRedisStat(REDIS_KEY_STAT_SUCCESS, successApiCalls);
            return result;
        } catch (Exception e) {
            errorCounter.increment();
            incrRedisStat(REDIS_KEY_STAT_FAILURE, failureApiCalls);
            cbRecordFailure();
            log.error("DeepSeek chatWithTools failed", e);
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            result.put("toolCalls", null);
            return result;
        } finally {
            sample.stop(latencyTimer);
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
                cbRecordFailure();
                return result;
            }
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray data = body.getJSONArray("data");
            if (data == null) {
                cbRecordFailure();
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
            cbRecordFailure();
            return result;
        } finally {
            concurrencyLimit.release();
        }
    }

    public float[] embedding(String text) {
        List<float[]> results = embeddings(java.util.Collections.singletonList(text));
        return results.isEmpty() ? null : results.get(0);
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