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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
public class DeepSeekClient {

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.timeout-ms:30000}")
    private int timeoutMs;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.deepseek.max-concurrent:5}")
    private int maxConcurrent;

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

    private static final Map<String, String> FALLBACK_ANSWERS = new ConcurrentHashMap<>();

    static {
        FALLBACK_ANSWERS.put("faq", "抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。");
        FALLBACK_ANSWERS.put("default", "AI 助手正在休息，请稍后再试。");
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

    public String routeModel(List<Map<String, Object>> messages) {
        String lastUserMessage = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).get("role"))) {
                lastUserMessage = (String) messages.get(i).get("content");
                break;
            }
        }
        if (lastUserMessage == null) return currentModel;
        String scene = "chat";
        if (lastUserMessage.contains("[图片:")) {
            scene = "vision";
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
        String[] routed = routeByScene(scene);
        if (routed != null) {
            log.info("Model routing via channel: scene={} -> model={}", scene, routed[2]);
            lastRoutedConfig.set(routed);
            return routed[2];
        }
        lastRoutedConfig.remove();
        if ("vision".equals(scene) && currentVisionModel != null && !currentVisionModel.isEmpty()) {
            log.info("Model routing: vision model: {}", currentVisionModel);
            return currentVisionModel;
        }
        if ("reasoning".equals(scene) && currentRoutingEnabled && currentReasonerModel != null) {
            return currentReasonerModel;
        }
        return currentModel;
    }

    private ThreadLocal<String[]> lastRoutedConfig = new ThreadLocal<>();

    public String[] getRoutedConfig() {
        return lastRoutedConfig.get();
    }

    public String getModel() {
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
        log.info("Embedding API key updated and persisted");
    }

    public void updateEmbeddingBaseUrl(String newUrl) {
        currentEmbBaseUrl = newUrl != null ? newUrl.replaceAll("/+$", "") : "";
        try { stringRedisTemplate.opsForValue().set(REDIS_KEY_EMB_BASEURL, currentEmbBaseUrl); } catch (Exception ignored) {}
        log.info("Embedding base URL updated and persisted: {}", currentEmbBaseUrl);
    }

    public void updateEmbeddingModel(String newModel) {
        if (newModel != null && !newModel.isEmpty()) {
            currentEmbModel = newModel;
            try { stringRedisTemplate.opsForValue().set(REDIS_KEY_EMB_MODEL, newModel); } catch (Exception ignored) {}
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
        try {
            HttpResponse response = HttpRequest.get(currentBaseUrl + "/models")
                    .header("Authorization", "Bearer " + currentApiKey)
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
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            requestCounter.increment();
            HttpResponse response = null;
            try {
                if (!concurrencyLimit.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("AI concurrent request limit reached");
                }
                JSONObject payload = new JSONObject();
                String routedModel = routeModel(messages);
                String[] routed = getRoutedConfig();
                String chatUrl = (routed != null) ? routed[0] : currentBaseUrl;
                String chatKey = (routed != null) ? routed[1] : currentApiKey;
                payload.set("model", routedModel);
                payload.set("messages", JSONUtil.parseArray(messages));
                payload.set("stream", true);
                payload.set("temperature", 0.3);
                payload.set("max_tokens", 2048);

                response = HttpRequest.post(chatUrl + "/chat/completions")
                        .header("Authorization", "Bearer " + chatKey)
                        .header("Content-Type", "application/json")
                        .body(payload.toString())
                        .timeout(timeoutMs)
                        .execute();

                int code = response.getStatus();
                if (code < 200 || code >= 300) {
                    errorCounter.increment();
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
                            JSONArray choices = obj.getJSONArray("choices");
                            if (choices == null || choices.isEmpty()) continue;
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
                onDone.accept(null);
                return null;
            } catch (Exception e) {
                errorCounter.increment();
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
            String origKey = currentApiKey, origUrl = currentBaseUrl, origModel = currentModel;
            currentApiKey = fallbackApiKey;
            currentBaseUrl = fallbackBaseUrl.isEmpty() ? origUrl : fallbackBaseUrl;
            currentModel = fallbackModel.isEmpty() ? origModel : fallbackModel;
            try {
                log.info("Trying fallback model: {}", currentModel);
                String result = doChat(messages);
                if (!FALLBACK_ANSWERS.get("faq").equals(result)) return result;
            } finally {
                currentApiKey = origKey;
                currentBaseUrl = origUrl;
                currentModel = origModel;
            }
        }
        return FALLBACK_ANSWERS.get("faq");
    }

    private String doChat(List<Map<String, Object>> messages) {
        Timer.Sample sample = Timer.start(meterRegistry);
        requestCounter.increment();
        try {
            if (!concurrencyLimit.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                return FALLBACK_ANSWERS.get("faq");
            }
            JSONObject payload = new JSONObject();
            String routedModel = routeModel(messages);
            String[] routed = getRoutedConfig();
            String chatUrl = (routed != null) ? routed[0] : currentBaseUrl;
            String chatKey = (routed != null) ? routed[1] : currentApiKey;
            payload.set("model", routedModel);
            payload.set("messages", JSONUtil.parseArray(messages));
            payload.set("stream", false);
            payload.set("temperature", 0.3);
            payload.set("max_tokens", 2048);

            HttpResponse response = HttpRequest.post(chatUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + chatKey)
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .timeout(timeoutMs)
                    .execute();
            int code = response.getStatus();
            if (code < 200 || code >= 300) {
                errorCounter.increment();
                log.error("DeepSeek API error: code={}, body={}", code, response.body());
                return FALLBACK_ANSWERS.get("faq");
            }
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray choices = body.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return FALLBACK_ANSWERS.get("faq");
            }
            return choices.getJSONObject(0).getJSONObject("message").getStr("content");
        } catch (Exception e) {
            errorCounter.increment();
            log.error("DeepSeek API call failed", e);
            return FALLBACK_ANSWERS.get("faq");
        } finally {
            sample.stop(latencyTimer);
            concurrencyLimit.release();
        }
    }

    public Map<String, Object> chatWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> result = new HashMap<>();
        if (!isEnabled()) {
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            result.put("toolCalls", null);
            return result;
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            result = doChatWithTools(messages, tools);
            String content = (String) result.get("content");
            if (content != null && !FALLBACK_ANSWERS.get("faq").equals(content)) {
                return result;
            }
            if (attempt == 0) log.info("Retrying DeepSeek chatWithTools after failure");
        }
        if (fallbackApiKey != null && !fallbackApiKey.isEmpty()) {
            String origKey = currentApiKey, origUrl = currentBaseUrl, origModel = currentModel;
            currentApiKey = fallbackApiKey;
            currentBaseUrl = fallbackBaseUrl.isEmpty() ? origUrl : fallbackBaseUrl;
            currentModel = fallbackModel.isEmpty() ? origModel : fallbackModel;
            try {
                log.info("Trying fallback model (tools): {}", currentModel);
                result = doChatWithTools(messages, tools);
                String content = (String) result.get("content");
                if (content != null && !FALLBACK_ANSWERS.get("faq").equals(content)) return result;
            } finally {
                currentApiKey = origKey;
                currentBaseUrl = origUrl;
                currentModel = origModel;
            }
        }
        return result;
    }

    private Map<String, Object> doChatWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> result = new HashMap<>();
        if (!isEnabled()) {
            result.put("content", FALLBACK_ANSWERS.get("faq"));
            result.put("toolCalls", null);
            return result;
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        requestCounter.increment();
        try {
            if (!concurrencyLimit.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                result.put("toolCalls", null);
                return result;
            }
            JSONObject payload = new JSONObject();
            String routedModel = routeModel(messages);
            String[] routed = getRoutedConfig();
            String chatUrl = (routed != null) ? routed[0] : currentBaseUrl;
            String chatKey = (routed != null) ? routed[1] : currentApiKey;
            payload.set("model", routedModel);
            payload.set("messages", JSONUtil.parseArray(messages));
            payload.set("stream", false);
            payload.set("temperature", 0.3);
            payload.set("max_tokens", 2048);
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
                log.error("DeepSeek API error: code={}, body={}", code, response.body());
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                result.put("toolCalls", null);
                return result;
            }
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray choices = body.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                result.put("content", FALLBACK_ANSWERS.get("faq"));
                result.put("toolCalls", null);
                return result;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            result.put("content", message.getStr("content"));
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            result.put("toolCalls", toolCalls != null && !toolCalls.isEmpty() ? JSONUtil.toList(toolCalls, Map.class) : null);
            return result;
        } catch (Exception e) {
            errorCounter.increment();
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
                return result;
            }
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray data = body.getJSONArray("data");
            if (data == null) return result;
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                JSONArray embedding = item.getJSONArray("embedding");
                float[] vec = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) {
                    vec[j] = embedding.getFloat(j);
                }
                result.add(vec);
            }
            return result;
        } catch (Exception e) {
            log.warn("Embedding API call failed: {}", e.getMessage());
            return result;
        } finally {
            concurrencyLimit.release();
        }
    }

    public float[] embedding(String text) {
        List<float[]> results = embeddings(java.util.Collections.singletonList(text));
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean isEmbeddingAvailable() {

        try {
            float[] test = embedding("测试");
            return test != null && test.length > 0;
        } catch (Exception e) {
            return false;
        }
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