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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

    @Autowired
    @Qualifier("aiTaskExecutor")
    private ThreadPoolTaskExecutor aiTaskExecutor;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private Counter requestCounter;
    private Counter errorCounter;
    private Timer latencyTimer;
    private Semaphore concurrencyLimit;
    private volatile String currentApiKey;

    private static final Map<String, String> FALLBACK_ANSWERS = new ConcurrentHashMap<>();

    static {
        FALLBACK_ANSWERS.put("faq", "抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。");
        FALLBACK_ANSWERS.put("default", "AI 助手正在休息，请稍后再试。");
    }

    @PostConstruct
    public void init() {
        concurrencyLimit = new Semaphore(maxConcurrent);
        currentApiKey = apiKey;
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
        refreshApiKeyFromRedis();
        return aiEnabled && currentApiKey != null && !currentApiKey.isEmpty();
    }

    public String getModel() {
        return model;
    }

    public void updateApiKey(String newKey) {
        if (newKey != null && !newKey.isEmpty()) {
            currentApiKey = newKey;
            stringRedisTemplate.opsForValue().set("ai:config:api-key", newKey);
            log.info("DeepSeek API key updated");
        }
    }

    private void refreshApiKeyFromRedis() {
        try {
            String redisKey = stringRedisTemplate.opsForValue().get("ai:config:api-key");
            if (redisKey != null && !redisKey.isEmpty() && !redisKey.equals(currentApiKey)) {
                currentApiKey = redisKey;
            }
        } catch (Exception ignored) {}
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
                payload.set("model", model);
                payload.set("messages", JSONUtil.parseArray(messages));
                payload.set("stream", true);
                payload.set("temperature", 0.7);

                response = HttpRequest.post(baseUrl + "/chat/completions")
                        .header("Authorization", "Bearer " + currentApiKey)
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
        Timer.Sample sample = Timer.start(meterRegistry);
        requestCounter.increment();
        try {
            if (!concurrencyLimit.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                return FALLBACK_ANSWERS.get("faq");
            }
            JSONObject payload = new JSONObject();
            payload.set("model", model);
            payload.set("messages", JSONUtil.parseArray(messages));
            payload.set("stream", false);
            payload.set("temperature", 0.7);

            HttpResponse response = HttpRequest.post(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + currentApiKey)
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

}