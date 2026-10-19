package com.campustrade.ai;

import com.campustrade.service.ai.DeepSeekClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

/**
 * DeepSeekClient 单元测试。
 * 测试目标：熔断器状态机——初始放行、连续失败达到阈值（CB_FAILURE_THRESHOLD=5）后熔断打开、
 * 恢复超时（CB_RECOVERY_TIMEOUT_MS=15000）后 half-open 探测 CAS 只放行 1 个请求、
 * cbRecordSuccess 重置全部状态、half-open 探测失败重新打开，以及 getApiStatus
 * 的状态分支与熔断状态变化监听器副作用、静态工具方法 cosineSim。
 * 依赖策略：StringRedisTemplate 用 Mockito mock（含 opsForValue/opsForHash），
 * MeterRegistry 用真实 SimpleMeterRegistry，熔断器私有状态通过反射读写。
 */
public class DeepSeekClientTest {

    private DeepSeekClient client;
    private StringRedisTemplate redisTemplate;
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOps = Mockito.mock(HashOperations.class);
    private List<String> cbStateEvents;

    @BeforeEach
    public void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(redisTemplate.opsForHash()).thenReturn(hashOps);

        client = new DeepSeekClient();
        ReflectionTestUtils.setField(client, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(client, "meterRegistry", new SimpleMeterRegistry());

        // 注册熔断状态变化监听器，验证状态机切换的副作用
        cbStateEvents = new ArrayList<>();
        client.addCbStateListener(cbStateEvents::add);
    }

    // ===== 熔断器私有方法反射辅助 =====

    private boolean cbAllowRequest() {
        return ReflectionTestUtils.invokeMethod(client, "cbAllowRequest");
    }

    private void cbRecordFailure(String reason) {
        ReflectionTestUtils.invokeMethod(client, "cbRecordFailure", reason);
    }

    private void cbRecordSuccess() {
        ReflectionTestUtils.invokeMethod(client, "cbRecordSuccess");
    }

    private int getCbFailureCount() {
        AtomicInteger counter = (AtomicInteger) ReflectionTestUtils.getField(client, "cbFailureCount");
        return counter.get();
    }

    private boolean getHalfOpenProbe() {
        AtomicBoolean probe = (AtomicBoolean) ReflectionTestUtils.getField(client, "halfOpenProbe");
        return probe.get();
    }

    private void setField(String name, Object value) {
        ReflectionTestUtils.setField(client, name, value);
    }

    /** 连续失败 5 次（CB_FAILURE_THRESHOLD）使熔断打开 */
    private void openCircuitBreakerNow() {
        for (int i = 0; i < 5; i++) {
            cbRecordFailure("余额不足");
        }
    }

    // ========== 熔断器状态机 ==========

    // Test scenario: 初始状态（closed）应放行请求且 isCircuitBreakerOpen 为 false
    @Test
    public void testInitialClosedStateAllowsRequest() {
        assertTrue(cbAllowRequest(), "初始状态应放行请求");
        assertFalse(client.isCircuitBreakerOpen());
        assertEquals(0, getCbFailureCount());
        assertFalse(getHalfOpenProbe());
    }

    // Test scenario: 连续失败未达阈值 5 次时保持 closed 放行，失败计数累计
    @Test
    public void testFailuresBelowThresholdKeepClosed() {
        for (int i = 0; i < 4; i++) {
            cbRecordFailure("网络连接超时");
        }
        assertEquals(4, getCbFailureCount());
        assertFalse(client.isCircuitBreakerOpen(), "未达阈值不应熔断");
        assertTrue(cbAllowRequest(), "未达阈值应继续放行");
    }

    // Test scenario: 连续失败达到阈值 5 次后熔断打开，cbAllowRequest 拒绝并通知 open 事件
    @Test
    public void testFailureThresholdOpensCircuitBreaker() {
        openCircuitBreakerNow();
        assertTrue(client.isCircuitBreakerOpen(), "达到阈值 5 次后应熔断");
        assertFalse(cbAllowRequest(), "熔断后应拒绝请求");
        assertEquals(5, getCbFailureCount());
        assertTrue(cbStateEvents.contains("open"), "熔断打开应通知监听器 open 事件");
    }

    // Test scenario: 熔断打开后未超过恢复超时 15s，请求仍被拒绝
    @Test
    public void testOpenCircuitRejectsBeforeRecoveryTimeout() {
        openCircuitBreakerNow();
        assertFalse(cbAllowRequest(), "熔断未超时不应放行");
        assertTrue(client.isCircuitBreakerOpen());
    }

    // Test scenario: 恢复超时（>15s）后进入半开，CAS 保证探测名额只放行 1 个请求
    @Test
    public void testHalfOpenProbeCasGrantsOnlyOneRequest() {
        openCircuitBreakerNow();
        setField("cbOpenTime", System.currentTimeMillis() - 16000L);

        assertTrue(cbAllowRequest(), "第一个探测请求应被放行");
        assertTrue(getHalfOpenProbe(), "探测名额应标记为已占用");
        assertFalse(cbAllowRequest(), "第二个请求应因探测名额被占用而被拒绝");
        assertFalse(cbAllowRequest(), "后续请求仍应被拒绝");
    }

    // Test scenario: 半开探测成功后 cbRecordSuccess 重置全部状态（计数清零、关闭熔断、探测名额复位）
    @Test
    public void testSuccessResetsAllCircuitBreakerState() {
        openCircuitBreakerNow();
        setField("cbOpenTime", System.currentTimeMillis() - 16000L);
        assertTrue(cbAllowRequest());

        cbRecordSuccess();

        assertEquals(0, getCbFailureCount(), "成功后失败计数应清零");
        assertFalse(getHalfOpenProbe(), "探测名额应复位");
        assertFalse(client.isCircuitBreakerOpen(), "熔断应关闭");
        assertTrue(cbAllowRequest(), "恢复后应放行请求");
        assertTrue(cbStateEvents.contains("closed"), "恢复应通知监听器 closed 事件");
    }

    // Test scenario: 半开探测失败后重新打开熔断，刷新时间窗口并释放探测名额
    @Test
    public void testHalfOpenProbeFailureReopensCircuitBreaker() {
        openCircuitBreakerNow();
        setField("cbOpenTime", System.currentTimeMillis() - 16000L);
        assertTrue(cbAllowRequest(), "半开探测应放行");

        cbRecordFailure("half-open probe failed");

        assertEquals(6, getCbFailureCount(), "半开失败应继续累计失败次数");
        assertFalse(getHalfOpenProbe(), "探测名额应被释放");
        assertTrue(client.isCircuitBreakerOpen(), "熔断应保持打开");
        assertFalse(cbAllowRequest(), "刚重新打开（时间窗口刷新）不应立即放行");

        // 超过新的 15s 窗口后应再次进入半开放行
        setField("cbOpenTime", System.currentTimeMillis() - 16000L);
        assertTrue(cbAllowRequest(), "新的恢复超时后应再次进入半开");
    }

    // Test scenario: 带原因的失败记录 lastFailureReason，无参重载不覆盖已有原因
    @Test
    public void testRecordFailureStoresReason() {
        cbRecordFailure("余额不足");
        assertEquals("余额不足", ReflectionTestUtils.getField(client, "lastFailureReason"));

        ReflectionTestUtils.invokeMethod(client, "cbRecordFailure");
        assertEquals("余额不足", ReflectionTestUtils.getField(client, "lastFailureReason"),
                "无参 cbRecordFailure 不应清空已有原因");
    }

    // Test scenario: 未熔断但有失败时监听器收到 degraded 事件（而非 open）
    @Test
    public void testDegradedEventNotifiedWhenFailuresBelowThreshold() {
        cbRecordFailure("API Key无效或已过期");
        assertTrue(cbStateEvents.contains("degraded"), "未熔断但有失败应通知 degraded");
        assertFalse(cbStateEvents.contains("open"), "未达阈值不应通知 open");
    }

    // ========== getApiStatus 状态分支 ==========

    // Test scenario: AI 未启用时 getApiStatus 返回 disabled
    @Test
    public void testGetApiStatusDisabledWhenAiDisabled() {
        setField("aiEnabled", false);
        Map<String, Object> status = client.getApiStatus();
        assertEquals("disabled", status.get("status"));
    }

    // Test scenario: 启用但 API Key 未配置时返回 disabled
    @Test
    public void testGetApiStatusDisabledWhenApiKeyMissing() {
        setField("aiEnabled", true);
        setField("currentApiKey", null);
        Map<String, Object> status = client.getApiStatus();
        assertEquals("disabled", status.get("status"));
    }

    // Test scenario: 熔断中且未超时返回 circuit_breaker，消息含失败原因并给出剩余秒数
    @Test
    public void testGetApiStatusCircuitBreakerWithinTimeout() {
        setField("aiEnabled", true);
        setField("currentApiKey", "test-key");
        setField("currentBaseUrl", "http://127.0.0.1:1");
        openCircuitBreakerNow();
        Map<String, Object> status = client.getApiStatus();
        assertEquals("circuit_breaker", status.get("status"));
        Long remaining = (Long) status.get("remainingSeconds");
        assertNotNull(remaining);
        assertTrue(remaining >= 0 && remaining <= 15, "剩余秒数应在 [0,15] 内, 实际=" + remaining);
        assertTrue(String.valueOf(status.get("message")).contains("熔断保护中"));
        assertTrue(String.valueOf(status.get("message")).contains("余额不足"), "消息应包含失败原因");
    }

    // Test scenario: 存在失败但未熔断时返回 degraded，包含失败原因与剩余熔断次数提示
    @Test
    public void testGetApiStatusDegradedWhenFailuresExist() {
        setField("aiEnabled", true);
        setField("currentApiKey", "test-key");
        cbRecordFailure("调用频率超限");
        Map<String, Object> status = client.getApiStatus();
        assertEquals("degraded", status.get("status"));
        String message = String.valueOf(status.get("message"));
        assertTrue(message.contains("调用频率超限"), "应包含失败原因, 实际=" + message);
        assertTrue(message.contains("4次后熔断"), "应提示距熔断剩余次数, 实际=" + message);
    }

    // Test scenario: 启用、无失败且无可用渠道（路由为空且 baseUrl 未配置）时返回 no_channel
    @Test
    public void testGetApiStatusNoChannelWhenNoRouteConfigured() {
        setField("aiEnabled", true);
        setField("currentApiKey", "test-key");
        setField("currentBaseUrl", null);
        Map<String, Object> status = client.getApiStatus();
        assertEquals("no_channel", status.get("status"));
        assertEquals("无可用渠道配置", status.get("message"));
    }

    // ========== 静态工具方法 ==========

    // Test scenario: cosineSim 相同向量相似度为 1
    @Test
    public void testCosineSimIdenticalVectors() {
        float[] v = {0.5f, 0.5f, 0.5f};
        assertEquals(1.0, DeepSeekClient.cosineSim(v, v), 1e-6);
    }

    // Test scenario: cosineSim 正交向量相似度为 0
    @Test
    public void testCosineSimOrthogonalVectors() {
        assertEquals(0.0, DeepSeekClient.cosineSim(new float[]{1f, 0f}, new float[]{0f, 1f}), 1e-6);
    }

    // Test scenario: cosineSim 零向量相似度为 0（避免除零）
    @Test
    public void testCosineSimZeroVectorReturnsZero() {
        assertEquals(0.0, DeepSeekClient.cosineSim(new float[]{0f, 0f}, new float[]{1f, 1f}), 1e-6);
    }

    // ==================== init 与状态恢复 ====================

    // Test scenario: init 无 Redis 缓存时使用默认值并初始化并发信号量
    @Test
    public void testInitLoadsWithDefaultsWhenRedisEmpty() {
        client.init();
        assertFalse(client.isEnabled(), "aiEnabled 默认 false 时不应启用");
        assertNotNull(ReflectionTestUtils.getField(client, "concurrencyLimit"), "应初始化并发信号量");
    }

    // Test scenario: init 从 Redis 恢复 API Key、模型与统计计数
    @Test
    public void testInitRestoresConfigAndStatsFromRedis() {
        Mockito.when(valueOps.get("ai:config:apikey")).thenReturn("sk-redis-key");
        Mockito.when(valueOps.get("ai:config:model")).thenReturn("deepseek-chat-2");
        Mockito.when(valueOps.get("ai:stats:api_calls")).thenReturn("100");
        Mockito.when(valueOps.get("ai:stats:success_calls")).thenReturn("90");
        client.init();
        assertEquals("sk-redis-key", ReflectionTestUtils.getField(client, "currentApiKey"));
        assertEquals("deepseek-chat-2", ReflectionTestUtils.getField(client, "currentModel"));
        Map<String, Object> stats = client.getApiStats();
        assertEquals(100L, stats.get("totalApiCalls"));
        assertEquals(90L, stats.get("successApiCalls"));
    }

    // Test scenario: init 恢复 Redis 熔断状态（未超时的 open 状态恢复为打开）
    @Test
    public void testInitRestoresOpenCircuitBreakerFromRedis() {
        Map<Object, Object> cbState = new HashMap<>();
        cbState.put("failCount", "3");
        cbState.put("open", "true");
        cbState.put("openTime", String.valueOf(System.currentTimeMillis()));
        cbState.put("reason", "余额不足");
        Mockito.when(hashOps.entries("ai:cb:state")).thenReturn(cbState);
        client.init();
        assertTrue(client.isCircuitBreakerOpen(), "未超时的 open 状态应恢复为打开");
        assertEquals(3, getCbFailureCount());
    }

    // Test scenario: init 恢复 Redis 熔断状态（已超时的 open 状态重置为关闭并删除状态）
    @Test
    public void testInitResetsExpiredOpenCircuitBreaker() {
        Map<Object, Object> cbState = new HashMap<>();
        cbState.put("failCount", "5");
        cbState.put("open", "true");
        cbState.put("openTime", String.valueOf(System.currentTimeMillis() - 60000L));
        Mockito.when(hashOps.entries("ai:cb:state")).thenReturn(cbState);
        client.init();
        assertFalse(client.isCircuitBreakerOpen(), "已超时的 open 状态应重置为关闭");
        assertEquals(0, getCbFailureCount());
        Mockito.verify(redisTemplate).delete("ai:cb:state");
    }

    // Test scenario: init 恢复 Redis 熔断状态（closed 状态只恢复失败计数）
    @Test
    public void testInitRestoresClosedCircuitBreakerFailCount() {
        Map<Object, Object> cbState = new HashMap<>();
        cbState.put("failCount", "2");
        cbState.put("open", "false");
        Mockito.when(hashOps.entries("ai:cb:state")).thenReturn(cbState);
        client.init();
        assertFalse(client.isCircuitBreakerOpen());
        assertEquals(2, getCbFailureCount());
    }

    // Test scenario: 监听器抛异常时不影响熔断状态机（达到阈值仍正常熔断）
    @Test
    public void testListenerExceptionDoesNotBreakStateMachine() {
        client.addCbStateListener(s -> {
            throw new RuntimeException("listener boom");
        });
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> cbRecordFailure("余额不足"));
        }
        assertTrue(client.isCircuitBreakerOpen(), "监听器异常不应影响熔断状态");
    }

    // ==================== 配置更新与掩码 ====================

    // Test scenario: isEnabled 要求 aiEnabled 与 API Key 同时满足
    @Test
    public void testIsEnabledRequiresFlagAndApiKey() {
        setField("aiEnabled", true);
        setField("currentApiKey", "");
        assertFalse(client.isEnabled());
        setField("currentApiKey", "sk-test");
        assertTrue(client.isEnabled());
    }

    // Test scenario: updateApiKey 更新并持久化，null 不更新
    @Test
    public void testUpdateApiKeyPersistsAndIgnoresBlank() {
        client.updateApiKey("new-api-key-123");
        assertEquals("new-api-key-123", ReflectionTestUtils.getField(client, "currentApiKey"));
        Mockito.verify(valueOps).set("ai:config:apikey", "new-api-key-123");
        client.updateApiKey(null);
        assertEquals("new-api-key-123", ReflectionTestUtils.getField(client, "currentApiKey"), "null 不应更新");
    }

    // Test scenario: updateModel 更新并持久化，空串不更新
    @Test
    public void testUpdateModelPersistsAndIgnoresBlank() {
        client.updateModel("deepseek-reasoner");
        assertEquals("deepseek-reasoner", client.getModel());
        Mockito.verify(valueOps).set("ai:config:model", "deepseek-reasoner");
        client.updateModel("");
        assertEquals("deepseek-reasoner", client.getModel(), "空串不应更新");
    }

    // Test scenario: updateBaseUrl 去除尾部斜杠后持久化
    @Test
    public void testUpdateBaseUrlStripsTrailingSlash() {
        client.updateBaseUrl("https://api.example.com/v1///");
        assertEquals("https://api.example.com/v1", client.getCurrentBaseUrl());
        Mockito.verify(valueOps).set("ai:config:baseUrl", "https://api.example.com/v1");
    }

    // Test scenario: getCurrentApiKeyMasked 对长 key 脱敏、短 key 返回空
    @Test
    public void testGetCurrentApiKeyMasked() {
        setField("currentApiKey", "sk-1234567890abcdef");
        assertEquals("sk-1****cdef", client.getCurrentApiKeyMasked());
        setField("currentApiKey", "short");
        assertEquals("", client.getCurrentApiKeyMasked(), "短 key 应返回空");
    }

    // Test scenario: getCurrentEmbApiKeyMasked 空/短/长三种形态
    @Test
    public void testGetCurrentEmbApiKeyMasked() {
        setField("currentEmbApiKey", "");
        assertEquals("", client.getCurrentEmbApiKeyMasked());
        setField("currentEmbApiKey", "abc");
        assertEquals("****", client.getCurrentEmbApiKeyMasked());
        setField("currentEmbApiKey", "emb-key-12345678");
        assertEquals("emb-****5678", client.getCurrentEmbApiKeyMasked());
    }

    // Test scenario: extractFailureReason 按 HTTP 状态码映射失败原因
    @Test
    public void testExtractFailureReasonMapsStatusCodes() {
        assertEquals("余额不足", ReflectionTestUtils.invokeMethod(client, "extractFailureReason", 402, ""));
        assertEquals("API Key无效或已过期", ReflectionTestUtils.invokeMethod(client, "extractFailureReason", 401, ""));
        assertEquals("调用频率超限", ReflectionTestUtils.invokeMethod(client, "extractFailureReason", 429, ""));
        assertEquals("API服务端错误(HTTP 500)", ReflectionTestUtils.invokeMethod(client, "extractFailureReason", 500, ""));
        assertEquals("HTTP 404", ReflectionTestUtils.invokeMethod(client, "extractFailureReason", 404, ""));
    }

    // Test scenario: extractExceptionReason 识别网络/超时异常、null 消息回退类名、超长截断
    @Test
    public void testExtractExceptionReasonClassifiesExceptions() {
        assertEquals("网络连接超时", ReflectionTestUtils.invokeMethod(client, "extractExceptionReason",
                new RuntimeException("Connection refused: localhost")));
        assertEquals("网络连接超时", ReflectionTestUtils.invokeMethod(client, "extractExceptionReason",
                new RuntimeException("Read timeout after 5s")));
        assertEquals("RuntimeException", ReflectionTestUtils.invokeMethod(client, "extractExceptionReason",
                new RuntimeException()));
        String longMsg = new String(new char[100]).replace('\0', 'x');
        String reason = ReflectionTestUtils.invokeMethod(client, "extractExceptionReason", new RuntimeException(longMsg));
        assertEquals(80, reason.length(), "超长消息应截断为 80 字符");
    }

    // ==================== getApiStats 统计 ====================

    // Test scenario: getApiStats 从 Redis 读取统计并计算成功率与平均延迟
    @Test
    public void testGetApiStatsComputesRatesFromRedis() {
        Mockito.when(valueOps.get("ai:stats:api_calls")).thenReturn("10");
        Mockito.when(valueOps.get("ai:stats:success_calls")).thenReturn("8");
        Mockito.when(valueOps.get("ai:stats:failure_calls")).thenReturn("2");
        Mockito.when(valueOps.get("ai:stats:total_tokens")).thenReturn("500");
        Mockito.when(valueOps.get("ai:stats:latency_sum")).thenReturn("1000");
        Mockito.when(valueOps.get("ai:stats:latency_count")).thenReturn("10");
        Map<String, Object> stats = client.getApiStats();
        assertEquals(10L, stats.get("totalApiCalls"));
        assertEquals(8L, stats.get("successApiCalls"));
        assertEquals(80.0, stats.get("successRate"));
        assertEquals(20.0, stats.get("failureRate"));
        assertEquals(100.0, stats.get("avgLatencyMs"));
    }

    // Test scenario: getApiStats 无统计时成功率为 0（三元表达式类型提升为 double）
    @Test
    public void testGetApiStatsZeroWhenNoCalls() {
        Map<String, Object> stats = client.getApiStats();
        assertEquals(0.0, stats.get("successRate"));
    }

    // ==================== chat/chatStream 兜底与失败路径 ====================

    // Test scenario: 未启用时 chat 返回 FAQ 兜底文案
    @Test
    public void testChatReturnsFallbackWhenDisabled() {
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。",
                client.chat(new ArrayList<>()));
    }

    // Test scenario: 未启用时 chatWithTools 返回兜底内容且无工具调用
    @Test
    public void testChatWithToolsReturnsFallbackWhenDisabled() {
        Map<String, Object> result = client.chatWithTools(new ArrayList<>(), new ArrayList<>());
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。", result.get("content"));
        assertNull(result.get("toolCalls"));
    }

    // Test scenario: 未启用时 chatWithToolsForScene 返回兜底内容
    @Test
    public void testChatWithToolsForSceneReturnsFallbackWhenDisabled() {
        Map<String, Object> result = client.chatWithToolsForScene(new ArrayList<>(), new ArrayList<>(), "chat");
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。", result.get("content"));
        assertNull(result.get("toolCalls"));
    }

    // Test scenario: 熔断打开时 chatStream 快速失败并回调 onError 与 onDone
    @Test
    public void testChatStreamFailsFastWhenCircuitBreakerOpen() {
        setField("aiEnabled", true);
        setField("currentApiKey", "sk-test");
        openCircuitBreakerNow();
        List<Throwable> errors = new ArrayList<>();
        List<Boolean> dones = new ArrayList<>();
        CompletableFuture<Void> future = client.chatStream(new ArrayList<>(), t -> { }, v -> dones.add(Boolean.TRUE), errors::add);
        assertNotNull(future);
        assertTrue(future.isDone(), "熔断拒绝应立即完成");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("熔断保护中"));
        assertEquals(1, dones.size(), "onDone 应被回调");
    }

    // Test scenario: 未启用时 chatStream 回调 IllegalStateException
    @Test
    public void testChatStreamFailsFastWhenDisabled() {
        List<Throwable> errors = new ArrayList<>();
        List<Boolean> dones = new ArrayList<>();
        client.chatStream(new ArrayList<>(), t -> { }, v -> dones.add(Boolean.TRUE), errors::add);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof IllegalStateException);
        assertEquals(1, dones.size());
    }

    // Test scenario: 启用且熔断关闭时 chat 同步执行，网络失败经重试与 fallback 后返回兜底文案
    @Test
    public void testChatRetriesAndFallsBackOnNetworkFailure() {
        setField("aiEnabled", true);
        setField("maxConcurrent", 5);
        setField("timeoutMs", 1000);
        client.init();
        // init 会用 @Value 字段(null)重置 current* 配置，须在其后重新注入
        setField("currentApiKey", "sk-test");
        setField("currentBaseUrl", "http://127.0.0.1:1");
        String result = client.chat(new ArrayList<>());
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。", result);
        assertEquals(2, getCbFailureCount(), "两次重试应累计 2 次失败");
    }

    // Test scenario: 启用时 chatWithTools 并发许可耗尽（Semaphore(0)）快速失败返回兜底
    @Test
    public void testChatWithToolsHandlesConcurrencyLimitFailure() {
        setField("aiEnabled", true);
        setField("currentApiKey", "sk-test");
        setField("currentBaseUrl", "http://127.0.0.1:1");
        client.init();
        // init 会用 @Value 字段(null)重置 current* 配置，须在其后重新注入
        setField("currentApiKey", "sk-test");
        setField("currentBaseUrl", "http://127.0.0.1:1");
        Map<String, Object> result = client.chatWithTools(new ArrayList<>(), new ArrayList<>());
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。", result.get("content"));
        assertNull(result.get("toolCalls"));
        // 并发许可耗尽快速失败，不触发熔断计数
        assertEquals(0, getCbFailureCount());
    }

    // ==================== 路由与渠道 ====================

    // Test scenario: routeByScene 按场景能力匹配启用渠道
    @Test
    public void testRouteBySceneMatchesEnabledChannel() {
        Mockito.when(valueOps.get("ai:channels")).thenReturn(
                "[{\"id\":\"ch1\",\"baseUrl\":\"https://api1.example.com\",\"apiKey\":\"key-1234567890\",\"enabled\":true,\"priority\":5},"
                        + "{\"id\":\"ch2\",\"baseUrl\":\"https://api2.example.com\",\"apiKey\":\"key-0987654321\",\"enabled\":true,\"priority\":1}]");
        Mockito.when(valueOps.get("ai:models")).thenReturn(
                "[{\"model\":\"deepseek-chat\",\"channelId\":\"ch1\",\"caps\":[\"chat\"]},"
                        + "{\"model\":\"deepseek-reasoner\",\"channelId\":\"ch2\",\"caps\":[\"reasoning\"]}]");
        String[] routed = client.routeByScene("chat");
        assertNotNull(routed);
        assertEquals("https://api1.example.com", routed[0]);
        assertEquals("deepseek-chat", routed[2]);
        String[] reasoning = client.routeByScene("reasoning");
        assertNotNull(reasoning);
        assertEquals("https://api2.example.com", reasoning[0]);
        assertEquals("deepseek-reasoner", reasoning[2]);
        assertNull(client.routeByScene("vision"), "无 vision 能力模型应返回 null");
    }

    // Test scenario: getEffectiveModel 无渠道路由时回退默认模型
    @Test
    public void testGetEffectiveModelFallsBackToDefault() {
        Mockito.when(valueOps.get("ai:channels")).thenReturn(null);
        Mockito.when(valueOps.get("ai:models")).thenReturn(null);
        setField("currentModel", "deepseek-chat");
        assertEquals("deepseek-chat", client.getEffectiveModel());
    }

    // Test scenario: getChannelsJson 对长 apiKey 脱敏，坏 JSON 与空缓存回退空数组
    @Test
    public void testGetChannelsJsonMasksKeysAndHandlesBadInput() {
        Mockito.when(valueOps.get("ai:channels")).thenReturn(
                "[{\"id\":\"ch1\",\"apiKey\":\"abcdefgh12345678\"}]");
        String json = client.getChannelsJson();
        assertFalse(json.contains("abcdefgh12345678"), "明文 key 不应返回");
        assertTrue(json.contains("abcd****5678"));
        Mockito.when(valueOps.get("ai:channels")).thenReturn("not-json");
        assertEquals("[]", client.getChannelsJson());
        Mockito.when(valueOps.get("ai:channels")).thenReturn(null);
        assertEquals("[]", client.getChannelsJson());
    }

    // Test scenario: getModelsJson 无缓存返回空数组
    @Test
    public void testGetModelsJsonDefaultsToEmpty() {
        Mockito.when(valueOps.get("ai:models")).thenReturn(null);
        assertEquals("[]", client.getModelsJson());
    }

    // ==================== embedding 可用性 ====================

    // Test scenario: embeddings 无 embedding key 时短路返回空列表
    @Test
    public void testEmbeddingsReturnsEmptyWithoutApiKey() {
        assertTrue(client.embeddings(Collections.singletonList("测试")).isEmpty());
    }

    // Test scenario: embedding 单数无 key 时返回 null
    @Test
    public void testEmbeddingReturnsNullWithoutApiKey() {
        assertNull(client.embedding("测试"));
    }

    // Test scenario: isEmbeddingAvailable 在 embedding 失败时返回 false 并缓存结果
    @Test
    public void testIsEmbeddingAvailableCachesFalseResult() {
        setField("currentEmbApiKey", "emb-key");
        setField("currentEmbBaseUrl", "http://127.0.0.1:1");
        setField("currentEmbModel", "emb-model");
        setField("maxConcurrent", 5);
        setField("timeoutMs", 1000);
        client.init();
        assertFalse(client.isEmbeddingAvailable(), "探测失败应返回 false");
        long callsBefore = ((AtomicLong) ReflectionTestUtils.getField(client, "totalApiCalls")).get();
        assertFalse(client.isEmbeddingAvailable(), "缓存期内应直接返回缓存结果");
        long callsAfter = ((AtomicLong) ReflectionTestUtils.getField(client, "totalApiCalls")).get();
        assertEquals(callsBefore, callsAfter, "缓存结果不应再次发起请求");
    }

    // ==================== 定时熔断检查 ====================

    // Test scenario: 熔断未打开时定时检查直接返回不触碰 Redis
    @Test
    public void testScheduledCheckSkipsWhenClosed() {
        assertFalse(client.isCircuitBreakerOpen());
        assertDoesNotThrow(() -> client.scheduledCircuitBreakerCheck());
        Mockito.verifyNoInteractions(hashOps);
    }

    // Test scenario: 熔断打开但未超时时定时检查不探测、不重置状态
    @Test
    public void testScheduledCheckSkipsBeforeTimeout() {
        openCircuitBreakerNow();
        assertDoesNotThrow(() -> client.scheduledCircuitBreakerCheck());
        assertEquals(5, getCbFailureCount(), "未超时不应重置失败计数");
        assertTrue(client.isCircuitBreakerOpen());
    }

    // ==================== chatAsync / chatStream 执行路径 ====================

    /** 同步执行的线程池替身：execute 直接运行任务 */
    private ThreadPoolTaskExecutor directExecutor() {
        return new ThreadPoolTaskExecutor() {
            @Override
            public void execute(Runnable task) {
                task.run();
            }
        };
    }

    // Test scenario: chatAsync 未启用时经线程池执行返回兜底文案
    @Test
    public void testChatAsyncReturnsFallbackWhenDisabled() throws Exception {
        ReflectionTestUtils.setField(client, "aiTaskExecutor", directExecutor());
        CompletableFuture<String> future = client.chatAsync(new ArrayList<>());
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。",
                future.get(5, TimeUnit.SECONDS));
    }

    // Test scenario: chatStream 经线程池执行，网络失败后回调 onError 并计入熔断
    @Test
    public void testChatStreamHandlesNetworkFailure() throws Exception {
        setField("aiEnabled", true);
        setField("maxConcurrent", 5);
        setField("streamTimeoutMs", 1000);
        client.init();
        // init 会用 @Value 字段(null)重置 current* 配置，须在其后重新注入
        setField("currentApiKey", "sk-test");
        setField("currentBaseUrl", "http://127.0.0.1:1");
        ReflectionTestUtils.setField(client, "aiTaskExecutor", directExecutor());
        List<Throwable> errors = new ArrayList<>();
        CompletableFuture<Void> future = client.chatStream(new ArrayList<>(), t -> { }, v -> { }, errors::add);
        future.get(15, TimeUnit.SECONDS);
        assertEquals(1, errors.size(), "网络失败应回调 onError");
        assertEquals(1, getCbFailureCount(), "网络失败应计入熔断");
    }

    // ==================== routeModel 意图分类 ====================

    // Test scenario: 消息中无 user 角色时直接返回默认配置
    @Test
    public void testRouteModelReturnsDefaultWithoutUserMessage() {
        setField("currentBaseUrl", "https://default.example.com");
        setField("currentApiKey", "sk-test");
        setField("currentModel", "deepseek-chat");
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("role", "system");
        m.put("content", "系统提示");
        messages.add(m);
        DeepSeekClient.RoutedConfig routed = client.routeModel(messages);
        assertEquals("deepseek-chat", routed.model);
        assertEquals("https://default.example.com", routed.baseUrl);
    }

    // Test scenario: routeModel 按关键词将分析类问题识别为 reasoning 场景，无渠道路由时回退默认模型
    @Test
    public void testRouteModelClassifiesReasoningByKeyword() {
        setField("currentBaseUrl", "https://default.example.com");
        setField("currentApiKey", "sk-test");
        setField("currentModel", "deepseek-chat");
        Mockito.when(valueOps.get("ai:channels")).thenReturn(null);
        Mockito.when(valueOps.get("ai:models")).thenReturn(null);
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("role", "user");
        m.put("content", "帮我分析一下销售趋势");
        messages.add(m);
        DeepSeekClient.RoutedConfig routed = client.routeModel(messages);
        assertEquals("https://default.example.com", routed.baseUrl);
        assertEquals("sk-test", routed.apiKey);
        assertEquals("deepseek-chat", routed.model, "无渠道路由时回退默认模型");
    }

    // Test scenario: routeModel 对含图片标记的消息走 vision 场景，无 vision 模型配置时回退默认
    @Test
    public void testRouteModelHandlesVisionMessage() {
        setField("currentBaseUrl", "https://default.example.com");
        setField("currentApiKey", "sk-test");
        setField("currentModel", "deepseek-chat");
        Mockito.when(valueOps.get("ai:channels")).thenReturn(null);
        Mockito.when(valueOps.get("ai:models")).thenReturn(null);
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("role", "user");
        m.put("content", "看看这个[图片:1](/uploads/a.png)是什么");
        messages.add(m);
        DeepSeekClient.RoutedConfig routed = client.routeModel(messages);
        assertEquals("deepseek-chat", routed.model, "无 vision 模型配置时回退默认");
    }

    // ==================== 渠道/模型保存与配置持久化 ====================

    // Test scenario: saveChannelsJson 保存渠道并保留掩码 key 的原值
    @Test
    public void testSaveChannelsJsonPreservesMaskedKeys() {
        Mockito.when(valueOps.get("ai:channels")).thenReturn(
                "[{\"id\":\"ch1\",\"apiKey\":\"real-secret-key\"}]");
        client.saveChannelsJson("[{\"id\":\"ch1\",\"apiKey\":\"abcd****cdef\"}]");
        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOps).set(eq("ai:channels"), captor.capture());
        assertFalse(captor.getValue().contains("abcd****cdef"), "掩码 key 应替换为原值");
        assertTrue(captor.getValue().contains("real-secret-key"));
    }

    // Test scenario: saveModelsJson 持久化模型注册 JSON
    @Test
    public void testSaveModelsJsonPersists() {
        client.saveModelsJson("[{\"model\":\"deepseek-chat\",\"caps\":[\"chat\"]}]");
        Mockito.verify(valueOps).set("ai:models", "[{\"model\":\"deepseek-chat\",\"caps\":[\"chat\"]}]");
    }

    // Test scenario: embedding 配置更新方法持久化
    @Test
    public void testUpdateEmbeddingConfigPersists() {
        client.updateEmbeddingApiKey("emb-key-new");
        assertEquals("emb-key-new", ReflectionTestUtils.getField(client, "currentEmbApiKey"));
        Mockito.verify(valueOps).set("ai:config:emb:apikey", "emb-key-new");
        client.updateEmbeddingBaseUrl("https://emb.example.com///");
        assertEquals("https://emb.example.com", ReflectionTestUtils.getField(client, "currentEmbBaseUrl"));
        client.updateEmbeddingModel("text-embedding-v2");
        assertEquals("text-embedding-v2", client.getCurrentEmbModel());
        Mockito.verify(valueOps).set("ai:config:emb:model", "text-embedding-v2");
    }

    // Test scenario: 路由开关/推理模型/视觉模型更新方法持久化
    @Test
    public void testUpdateRoutingAndModelsPersists() {
        client.updateRoutingEnabled(true);
        assertTrue(client.isCurrentRoutingEnabled());
        Mockito.verify(valueOps).set("ai:config:routing:enabled", "true");
        client.updateReasonerModel("deepseek-reasoner-v2");
        assertEquals("deepseek-reasoner-v2", client.getCurrentReasonerModel());
        Mockito.verify(valueOps).set("ai:config:routing:reasoner", "deepseek-reasoner-v2");
        client.updateVisionModel("qwen-vl");
        assertEquals("qwen-vl", client.getCurrentVisionModel());
        Mockito.verify(valueOps).set("ai:config:vision:model", "qwen-vl");
    }

    // ==================== chatWithTools 图片消息转换 ====================

    // Test scenario: chatWithTools 对非视觉模型的图片消息剥离图片标记
    @Test
    public void testChatWithToolsStripsImageMarkersForNonVisionModel() {
        setField("aiEnabled", true);
        setField("maxConcurrent", 0);
        client.init();
        setField("currentApiKey", "sk-test");
        setField("currentBaseUrl", "http://127.0.0.1:1");
        Mockito.when(valueOps.get("ai:channels")).thenReturn(null);
        Mockito.when(valueOps.get("ai:models")).thenReturn("[]");
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("role", "user");
        m.put("content", "看看这个[图片:1](/uploads/a.png)");
        messages.add(m);
        Map<String, Object> result = client.chatWithTools(messages, new ArrayList<>());
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。", result.get("content"));
    }

    // Test scenario: 视觉模型时图片标记转多模态格式（文件不存在时仅保留文本）
    @Test
    public void testChatWithToolsConvertsImageForVisionModel() {
        setField("aiEnabled", true);
        setField("uploadBasePath", "C:/nonexistent-uploads");
        setField("urlPrefix", "/uploads");
        setField("maxConcurrent", 0);
        client.init();
        setField("currentApiKey", "sk-test");
        setField("currentBaseUrl", "http://127.0.0.1:1");
        Mockito.when(valueOps.get("ai:channels")).thenReturn(null);
        Mockito.when(valueOps.get("ai:models")).thenReturn(
                "[{\"model\":\"deepseek-chat\",\"caps\":[\"chat\",\"vision\"]}]");
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("role", "user");
        m.put("content", "看看这个[图片:1](/uploads/a.png)");
        messages.add(m);
        Map<String, Object> result = client.chatWithTools(messages, new ArrayList<>());
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。", result.get("content"));
    }

    // Test scenario: chatWithToolsForScene 按场景覆盖路由（并发许可耗尽返回兜底）
    @Test
    public void testChatWithToolsForSceneRoutesBySceneOverride() {
        setField("aiEnabled", true);
        setField("maxConcurrent", 0);
        client.init();
        setField("currentApiKey", "sk-test");
        Mockito.when(valueOps.get("ai:channels")).thenReturn(
                "[{\"id\":\"ch1\",\"baseUrl\":\"https://api1.example.com\",\"apiKey\":\"key-1234567890\",\"enabled\":true,\"priority\":1}]");
        Mockito.when(valueOps.get("ai:models")).thenReturn(
                "[{\"model\":\"deepseek-chat\",\"channelId\":\"ch1\",\"caps\":[\"chat\"]}]");
        Map<String, Object> result = client.chatWithToolsForScene(new ArrayList<>(), new ArrayList<>(), "chat");
        assertEquals("抱歉，AI 服务暂时不可用，请稍后再试。您也可以联系客服获取帮助。", result.get("content"));
        Mockito.verify(valueOps).get("ai:channels");
    }
}