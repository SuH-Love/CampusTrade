package com.campustrade.ai;

import com.campustrade.service.ai.AiConfigService;
import com.campustrade.service.ai.DeepSeekClient;
import com.campustrade.service.ai.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * SessionService 单元测试。
 * 测试目标：静态 token 估算方法 estimateTokens 的字符分类与向上取整逻辑
 * （CJK 表意区字符按 1.5 token/字，其他字符按 0.25 token/字符，结果向上取整）。
 * 该方法为纯静态方法，无外部依赖，直接调用验证。
 */
public class SessionServiceTest {

    // Test scenario: 空字符串无任何字符，估算结果为 0
    @Test
    public void testEstimateTokensReturnsZeroForEmptyString() {
        assertEquals(0, SessionService.estimateTokens(""));
    }

    // Test scenario: 纯中文按 1.5 token/字估算并向上取整（5字 -> ceil(7.5)=8）
    @Test
    public void testEstimateTokensForPureChineseText() {
        assertEquals(8, SessionService.estimateTokens("你好世界啊"));
    }

    // Test scenario: 单个中文字向上取整（ceil(1.5)=2）
    @Test
    public void testEstimateTokensSingleChineseChar() {
        assertEquals(2, SessionService.estimateTokens("你"));
    }

    // Test scenario: 两个中文字整除边界（ceil(3.0)=3）
    @Test
    public void testEstimateTokensTwoChineseCharsExactValue() {
        assertEquals(3, SessionService.estimateTokens("好的"));
    }

    // Test scenario: 纯英文按 0.25 token/字符估算并向上取整（5字符 -> ceil(1.25)=2）
    @Test
    public void testEstimateTokensForPureEnglishText() {
        assertEquals(2, SessionService.estimateTokens("hello"));
    }

    // Test scenario: 纯数字按 0.25 token/字符估算并向上取整（10字符 -> ceil(2.5)=3）
    @Test
    public void testEstimateTokensForDigits() {
        assertEquals(3, SessionService.estimateTokens("1234567890"));
    }

    // Test scenario: 中英混合按分类加权后向上取整（2中文+3英文 -> ceil(3.75)=4）
    @Test
    public void testEstimateTokensForMixedText() {
        assertEquals(4, SessionService.estimateTokens("你好abc"));
    }

    // Test scenario: 单个英文字符向上取整为 1（ceil(0.25)=1）
    @Test
    public void testEstimateTokensSingleAsciiChar() {
        assertEquals(1, SessionService.estimateTokens("a"));
    }

    // Test scenario: 中文句号（。U+3002）不在 CJK 表意区（0x4E00-0x9FFF），按其他字符 0.25 计算（2中文+1其他 -> ceil(3.25)=4）
    @Test
    public void testEstimateTokensChinesePunctuationCountsAsOther() {
        assertEquals(4, SessionService.estimateTokens("你好。"));
    }

    // Test scenario: 当前实现无 null 保护，null 输入抛 NullPointerException（回归用例）
    @Test
    public void testEstimateTokensThrowsNpeForNullInput() {
        assertThrows(NullPointerException.class, () -> SessionService.estimateTokens(null));
    }

    // ==================== 实例方法测试（依赖 mock 注入） ====================

    private SessionService sessionService;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOps;
    private ListOperations<String, String> listOps;
    private DeepSeekClient deepSeekClient;
    private AiConfigService configService;

    @BeforeEach
    public void setUpInstance() {
        stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        listOps = Mockito.mock(ListOperations.class);
        deepSeekClient = Mockito.mock(DeepSeekClient.class);
        configService = Mockito.mock(AiConfigService.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(stringRedisTemplate.opsForList()).thenReturn(listOps);
        sessionService = new SessionService();
        ReflectionTestUtils.setField(sessionService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(sessionService, "deepSeekClient", deepSeekClient);
        ReflectionTestUtils.setField(sessionService, "configService", configService);
        // @Value 字段在无容器环境下默认值为 0，手动设置为生产默认值
        ReflectionTestUtils.setField(sessionService, "maxHistory", 20);
        ReflectionTestUtils.setField(sessionService, "sessionTtlHours", 168L);
    }

    // Test scenario: getHistory 解析 Redis 中的 JSON 消息列表并返回角色与内容
    @Test
    public void testGetHistoryParsesStoredMessages() {
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(Arrays.asList(
                "{\"role\":\"user\",\"content\":\"你好\"}",
                "{\"role\":\"assistant\",\"content\":\"您好\"}"));
        List<Map<String, Object>> history = sessionService.getHistory("s1");
        assertEquals(2, history.size());
        assertEquals("user", history.get(0).get("role"));
        assertEquals("你好", history.get(0).get("content"));
        assertEquals("assistant", history.get(1).get("role"));
    }

    // Test scenario: getHistory Redis 无数据时返回空列表
    @Test
    public void testGetHistoryReturnsEmptyWhenNoData() {
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(null);
        assertTrue(sessionService.getHistory("s1").isEmpty());
    }

    // Test scenario: getHistory 跳过无法解析的坏 JSON 行
    @Test
    public void testGetHistorySkipsMalformedJson() {
        Mockito.when(listOps.range("ai:session:s1", 0, -1))
                .thenReturn(Arrays.asList("not-json", "{\"role\":\"user\",\"content\":\"ok\"}"));
        List<Map<String, Object>> history = sessionService.getHistory("s1");
        assertEquals(1, history.size());
        assertEquals("ok", history.get(0).get("content"));
    }

    // Test scenario: addMessage 序列化消息后推送、按 maxHistory 裁剪并设置过期时间
    @Test
    public void testAddMessagePushesTrimsAndExpires() {
        sessionService.addMessage("s1", "user", "你好");
        Mockito.verify(listOps).rightPush(eq("ai:session:s1"), anyString());
        Mockito.verify(listOps).trim(eq("ai:session:s1"), eq(-40L), eq(-1L));
        Mockito.verify(stringRedisTemplate).expire(eq("ai:session:s1"), eq(168L), eq(TimeUnit.HOURS));
    }

    // Test scenario: addMessage Redis 异常时吞掉异常不外抛
    @Test
    public void testAddMessageSwallowsRedisException() {
        Mockito.when(listOps.rightPush(anyString(), anyString())).thenThrow(new RuntimeException("redis down"));
        assertDoesNotThrow(() -> sessionService.addMessage("s1", "user", "你好"));
    }

    // Test scenario: clearSession 删除对应会话 key
    @Test
    public void testClearSessionDeletesKey() {
        sessionService.clearSession("s1");
        Mockito.verify(stringRedisTemplate).delete("ai:session:s1");
    }

    // Test scenario: 消息数>=2 时移除最后一对消息（两次 pop）
    @Test
    public void testRemoveLastMessagePairPopsTwice() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(4L);
        sessionService.removeLastMessagePair("s1");
        Mockito.verify(listOps, Mockito.times(2)).rightPop("ai:session:s1");
    }

    // Test scenario: 消息数<2 时不移除任何消息
    @Test
    public void testRemoveLastMessagePairSkipsWhenTooFew() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(1L);
        sessionService.removeLastMessagePair("s1");
        Mockito.verify(listOps, Mockito.never()).rightPop(anyString());
    }

    // Test scenario: getLongTermMemory 读取摘要 key，Redis 异常时返回 null
    @Test
    public void testGetLongTermMemoryReadsAndSwallowsException() {
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn("摘要内容");
        assertEquals("摘要内容", sessionService.getLongTermMemory("s1"));
        Mockito.when(valueOps.get("ai:session:summary:s2")).thenThrow(new RuntimeException("down"));
        assertNull(sessionService.getLongTermMemory("s2"));
    }

    // Test scenario: getPreferences 读取偏好 key，Redis 异常时返回 null
    @Test
    public void testGetPreferencesReadsAndSwallowsException() {
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn("关注类别:二手书");
        assertEquals("关注类别:二手书", sessionService.getPreferences("s1"));
        Mockito.when(valueOps.get("ai:session:prefs:s2")).thenThrow(new RuntimeException("down"));
        assertNull(sessionService.getPreferences("s2"));
    }

    // Test scenario: savePreferences 写入并设置过期时间
    @Test
    public void testSavePreferencesWritesAndExpires() {
        sessionService.savePreferences("s1", "关注类别:二手书");
        Mockito.verify(valueOps).set("ai:session:prefs:s1", "关注类别:二手书");
        Mockito.verify(stringRedisTemplate).expire("ai:session:prefs:s1", 168L, TimeUnit.HOURS);
    }

    // Test scenario: extractAndSavePreferences 提取类别与价格关键词并去重保存
    @Test
    public void testExtractAndSavePreferencesExtractsKeywords() {
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        sessionService.extractAndSavePreferences("s1", "我想买二手书和电脑，最好便宜一点的");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOps).set(eq("ai:session:prefs:s1"), captor.capture());
        String saved = captor.getValue();
        assertTrue(saved.contains("关注类别:二手书"), "应提取类别关键词, 实际=" + saved);
        assertTrue(saved.contains("关注类别:电脑"), "应提取类别关键词");
        assertTrue(saved.contains("价格偏好:便宜"), "应提取价格关键词");
    }

    // Test scenario: 无关键词命中时不保存偏好
    @Test
    public void testExtractAndSavePreferencesSkipsWhenNoKeyword() {
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        sessionService.extractAndSavePreferences("s1", "今天天气不错");
        Mockito.verify(valueOps, Mockito.never()).set(anyString(), anyString());
    }

    // Test scenario: null/空消息不处理偏好
    @Test
    public void testExtractAndSavePreferencesSkipsNullEmpty() {
        sessionService.extractAndSavePreferences("s1", null);
        sessionService.extractAndSavePreferences("s1", "");
        Mockito.verifyNoInteractions(valueOps);
    }

    // Test scenario: saveSummary 追加已有摘要并写入 Redis 与过期时间
    @Test
    public void testSaveSummaryAppendsExisting() {
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn("旧摘要");
        Mockito.when(configService.getInt("session", "max_summary_length", 2000)).thenReturn(2000);
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(false);
        sessionService.saveSummary("s1", "新摘要");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOps).set(eq("ai:session:summary:s1"), captor.capture());
        assertEquals("旧摘要\n新摘要", captor.getValue());
        Mockito.verify(stringRedisTemplate).expire("ai:session:summary:s1", 168L, TimeUnit.HOURS);
    }

    // Test scenario: saveSummary 超长时截断保留尾部并从换行后开始
    @Test
    public void testSaveSummaryTruncatesLongSummary() {
        StringBuilder old = new StringBuilder();
        for (int i = 0; i < 1900; i++) {
            old.append("旧");
        }
        old.append("\n尾巴");
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn(old.toString());
        Mockito.when(configService.getInt("session", "max_summary_length", 2000)).thenReturn(2000);
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(false);
        StringBuilder sum = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sum.append("新");
        }
        sessionService.saveSummary("s1", sum.toString());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOps).set(eq("ai:session:summary:s1"), captor.capture());
        String saved = captor.getValue();
        // 实际截断行为：保留尾部 2000 字符后从其中第一个换行后开始，"尾巴"位于截断区域内被保留
        assertEquals(303, saved.length(), "截断后应不超过上限并从换行处截开");
        assertTrue(saved.endsWith(sum.toString()), "应以新摘要结尾");
        assertTrue(saved.startsWith("尾巴"), "截断区域内第一个换行后的内容被保留");
    }

    // Test scenario: applySummary 保留最近消息、重写历史并更新长期记忆
    @Test
    public void testApplySummaryRewritesHistoryKeepingRecent() {
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn(null);
        Mockito.when(configService.getInt("session", "max_summary_length", 2000)).thenReturn(2000);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(1);
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(false);
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(Arrays.asList(
                "{\"role\":\"user\",\"content\":\"消息0\"}",
                "{\"role\":\"assistant\",\"content\":\"消息1\"}",
                "{\"role\":\"user\",\"content\":\"消息2\"}",
                "{\"role\":\"assistant\",\"content\":\"消息3\"}"));
        sessionService.applySummary("s1", "会话摘要");
        Mockito.verify(valueOps).set("ai:session:summary:s1", "会话摘要");
        Mockito.verify(stringRedisTemplate).delete("ai:session:s1");
        Mockito.verify(listOps, Mockito.times(2)).rightPush(eq("ai:session:s1"), anyString());
        Mockito.verify(stringRedisTemplate).expire(eq("ai:session:s1"), eq(168L), eq(TimeUnit.HOURS));
    }

    // Test scenario: 消息数超过保留阈值时应触发摘要
    @Test
    public void testShouldSummarizeWhenMessageCountExceeds() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(30L);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        assertTrue(sessionService.shouldSummarize("s1"));
    }

    // Test scenario: 消息数不足但 token 总量超过 70% 上下文窗口时触发摘要
    @Test
    public void testShouldSummarizeWhenTokensExceed() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(10L);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(100);
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            content.append("长");
        }
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(
                Collections.singletonList("{\"role\":\"user\",\"content\":\"" + content + "\"}"));
        // 60个中文字 -> ceil(60*1.5)=90 > 100*0.7=70
        assertTrue(sessionService.shouldSummarize("s1"));
    }

    // Test scenario: 消息数与 token 均未超阈值时不触发摘要
    @Test
    public void testShouldSummarizeFalseWhenUnderThreshold() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(10L);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(1000);
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(
                Collections.singletonList("{\"role\":\"user\",\"content\":\"短消息\"}"));
        assertFalse(sessionService.shouldSummarize("s1"));
    }

    // Test scenario: Redis 返回 null 大小时不触发摘要
    @Test
    public void testShouldSummarizeFalseWhenSizeNull() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(null);
        assertFalse(sessionService.shouldSummarize("s1"));
    }

    // Test scenario: 待摘要消息不足时 prepareSummaryContext 返回 null
    @Test
    public void testPrepareSummaryContextReturnsNullWhenInsufficient() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(10L);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        assertNull(sessionService.prepareSummaryContext("s1", "请总结"));
    }

    // Test scenario: 待摘要消息充足时构建包含历史消息的摘要上下文
    @Test
    public void testPrepareSummaryContextBuildsContext() {
        Mockito.when(listOps.size("ai:session:s1")).thenReturn(30L);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        List<String> raw = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            raw.add("{\"role\":\"" + (i % 2 == 0 ? "user" : "assistant") + "\",\"content\":\"消息" + i + "\"}");
        }
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(raw);
        String ctx = sessionService.prepareSummaryContext("s1", "请总结以下对话");
        assertNotNull(ctx);
        assertTrue(ctx.startsWith("请总结以下对话"), "应以摘要提示开头");
        assertTrue(ctx.contains("user: 消息0"), "应包含待摘要历史消息");
        assertTrue(ctx.contains("assistant: 消息3"), "应包含待摘要历史消息");
        assertFalse(ctx.contains("消息23"), "最近 keepCount 条不应进入待摘要上下文");
    }

    // Test scenario: 两参 buildMessages 纯内存构建 system+user 消息
    @Test
    public void testBuildMessagesSimpleOverload() {
        List<Map<String, Object>> messages = sessionService.buildMessages("系统提示", "用户问题");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("系统提示", messages.get(0).get("content"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals("用户问题", messages.get(1).get("content"));
    }

    // Test scenario: 四参 buildMessages 组装长期记忆、用户偏好与历史消息
    @Test
    public void testBuildMessagesAssemblesMemoryPrefsAndHistory() {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(false);
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn("旧摘要");
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn("关注类别:二手书");
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(Arrays.asList(
                "{\"role\":\"user\",\"content\":\"你好\"}",
                "{\"role\":\"assistant\",\"content\":\"您好\"}"));
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(4000);

        List<Map<String, Object>> messages = sessionService.buildMessages("s1", "系统提示", "新问题");

        assertEquals(6, messages.size(), "system+记忆+偏好+2条历史+user 共6条");
        assertEquals("系统提示", messages.get(0).get("content"));
        assertTrue(String.valueOf(messages.get(1).get("content")).contains("旧摘要"), "应包含长期记忆");
        assertTrue(String.valueOf(messages.get(2).get("content")).contains("关注类别:二手书"), "应包含偏好");
        assertEquals("你好", messages.get(3).get("content"));
        assertEquals("您好", messages.get(4).get("content"));
        assertEquals("新问题", messages.get(5).get("content"));
    }

    // Test scenario: addMessagePair 通过 pipeline 写入用户与助手消息对
    @Test
    public void testAddMessagePairWritesViaPipeline() {
        org.springframework.data.redis.connection.RedisConnection connection =
                Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
        Mockito.doAnswer(inv -> {
            org.springframework.data.redis.core.RedisCallback<Object> callback = inv.getArgument(0);
            callback.doInRedis(connection);
            return null;
        }).when(stringRedisTemplate).executePipelined(Mockito.any(org.springframework.data.redis.core.RedisCallback.class));
        Mockito.when(stringRedisTemplate.getStringSerializer())
                .thenReturn(new org.springframework.data.redis.serializer.StringRedisSerializer());

        sessionService.addMessagePair("s1", "你好", "您好");

        Mockito.verify(connection, Mockito.times(2)).rPush(Mockito.any(byte[].class), Mockito.any(byte[].class));
    }

    // Test scenario: addMessagePair 携带思考步骤与工具调用时写入 assistant 消息
    @Test
    public void testAddMessagePairWithThinkingStepsAndToolCalls() {
        org.springframework.data.redis.connection.RedisConnection connection =
                Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
        Mockito.doAnswer(inv -> {
            org.springframework.data.redis.core.RedisCallback<Object> callback = inv.getArgument(0);
            callback.doInRedis(connection);
            return null;
        }).when(stringRedisTemplate).executePipelined(Mockito.any(org.springframework.data.redis.core.RedisCallback.class));
        Mockito.when(stringRedisTemplate.getStringSerializer())
                .thenReturn(new org.springframework.data.redis.serializer.StringRedisSerializer());
        List<Map<String, Object>> steps = new ArrayList<>();
        Map<String, Object> step = new HashMap<>();
        step.put("desc", "解析问题");
        steps.add(step);
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "create_order");
        tools.add(tool);

        sessionService.addMessagePair("s1", "查订单", "好的", steps, tools);

        ArgumentCaptor<byte[]> keyCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<byte[]> valCaptor = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(connection, Mockito.times(2)).rPush(keyCaptor.capture(), valCaptor.capture());
        String assistantJson = new String(valCaptor.getAllValues().get(1), StandardCharsets.UTF_8);
        assertTrue(assistantJson.contains("thinkingSteps"), "assistant 消息应包含思考步骤");
        assertTrue(assistantJson.contains("toolCalls"), "assistant 消息应包含工具调用");
    }

    // Test scenario: addMessagePair pipeline 异常时吞掉不外抛
    @Test
    public void testAddMessagePairSwallowsException() {
        Mockito.when(stringRedisTemplate.getStringSerializer())
                .thenReturn(new org.springframework.data.redis.serializer.StringRedisSerializer());
        Mockito.when(stringRedisTemplate.executePipelined(Mockito.any(org.springframework.data.redis.core.RedisCallback.class)))
                .thenThrow(new RuntimeException("pipeline failed"));
        assertDoesNotThrow(() -> sessionService.addMessagePair("s1", "你好", "您好"));
    }

    // Test scenario: saveSummary 在 embedding 可用时保存摘要向量块
    @Test
    public void testSaveSummarySavesEmbeddingChunkWhenAvailable() {
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn(null);
        Mockito.when(configService.getInt("session", "max_summary_length", 2000)).thenReturn(2000);
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        Mockito.when(deepSeekClient.embedding("新摘要")).thenReturn(new float[]{1f, 0f});
        Mockito.when(valueOps.get("ai:session:summary:vec:s1")).thenReturn(null);

        sessionService.saveSummary("s1", "新摘要");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOps).set(eq("ai:session:summary:vec:s1"), captor.capture());
        assertTrue(captor.getValue().contains("新摘要"), "向量块应包含摘要文本");
        Mockito.verify(stringRedisTemplate).expire(eq("ai:session:summary:vec:s1"), eq(168L), eq(TimeUnit.HOURS));
    }

    // Test scenario: 摘要向量块满 20 条时移除最旧的一条
    @Test
    public void testSaveSummaryEmbeddingEvictsOldestChunk() throws Exception {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        Mockito.when(deepSeekClient.embedding("新摘要")).thenReturn(new float[]{1f, 0f});
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn(null);
        Mockito.when(configService.getInt("session", "max_summary_length", 2000)).thenReturn(2000);
        List<Map<String, Object>> chunks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Map<String, Object> c = new HashMap<>();
            c.put("text", "旧" + i);
            c.put("emb", new float[]{1f, 0f});
            chunks.add(c);
        }
        Mockito.when(valueOps.get("ai:session:summary:vec:s1"))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(chunks));

        sessionService.saveSummary("s1", "新摘要");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOps).set(eq("ai:session:summary:vec:s1"), captor.capture());
        assertFalse(captor.getValue().contains("旧0"), "满额后最旧的块应被移除");
        assertTrue(captor.getValue().contains("新摘要"), "新块应被追加");
    }

    // Test scenario: buildMessages 在 embedding 可用时检索相关摘要上下文
    @Test
    public void testBuildMessagesRetrievesRelevantSummaryContext() throws Exception {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        List<Map<String, Object>> chunks = new ArrayList<>();
        Map<String, Object> c = new HashMap<>();
        c.put("text", "相关摘要内容");
        c.put("emb", new float[]{1f, 0f});
        chunks.add(c);
        Mockito.when(valueOps.get("ai:session:summary:vec:s1"))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(chunks));
        Mockito.when(deepSeekClient.embedding("新问题")).thenReturn(new float[]{1f, 0f});
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(null);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(4000);

        List<Map<String, Object>> messages = sessionService.buildMessages("s1", "系统提示", "新问题");

        assertEquals(3, messages.size(), "system+相关摘要+user");
        assertTrue(String.valueOf(messages.get(1).get("content")).contains("相关摘要内容"), "应包含相关摘要上下文");
    }

    // Test scenario: 摘要相关度低于 0.3 时回退长期记忆
    @Test
    public void testBuildMessagesFallsBackToLongTermMemoryWhenScoreLow() throws Exception {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        List<Map<String, Object>> chunks = new ArrayList<>();
        Map<String, Object> c = new HashMap<>();
        c.put("text", "无关摘要");
        c.put("emb", new float[]{0f, 1f});
        chunks.add(c);
        Mockito.when(valueOps.get("ai:session:summary:vec:s1"))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(chunks));
        Mockito.when(deepSeekClient.embedding("新问题")).thenReturn(new float[]{1f, 0f});
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn("旧长期记忆");
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(null);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(4000);

        List<Map<String, Object>> messages = sessionService.buildMessages("s1", "系统提示", "新问题");

        assertEquals(3, messages.size());
        assertTrue(String.valueOf(messages.get(1).get("content")).contains("旧长期记忆"), "低分应回退长期记忆");
    }

    // Test scenario: 摘要向量块为空时不注入记忆消息
    @Test
    public void testBuildMessagesSkipsMemoryWhenNoChunks() {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        Mockito.when(valueOps.get("ai:session:summary:vec:s1")).thenReturn("[]");
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(null);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(4000);

        List<Map<String, Object>> messages = sessionService.buildMessages("s1", "系统提示", "新问题");

        assertEquals(2, messages.size(), "向量块为空时仅 system+user");
    }

    // Test scenario: 历史消息 token 超限时从最早超限处截断，仅保留最近未超限部分
    @Test
    public void testBuildMessagesTruncatesHistoryByTokens() {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(false);
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn(null);
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(20);
        List<String> raw = new ArrayList<>();
        String longContent = String.join("", Collections.nCopies(10, "长"));
        for (int i = 0; i < 6; i++) {
            raw.add("{\"role\":\"user\",\"content\":\"" + longContent + "\"}");
        }
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(raw);

        List<Map<String, Object>> messages = sessionService.buildMessages("s1", "系统提示", "新问题");

        // 每条历史 10 个中文字 = ceil(15)=15 token；累计 30 > 20 时截断，保留最近 1 条
        assertEquals(3, messages.size(), "system+1条历史+user");
    }

    // ==================== embedding 返回 null 的回退路径 ====================

    // Test scenario: embedding 可用但查询向量返回 null 时回退长期记忆
    @Test
    public void testBuildMessagesFallsBackToLongTermMemoryWhenEmbeddingReturnsNull() throws Exception {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        List<Map<String, Object>> chunks = new ArrayList<>();
        Map<String, Object> c = new HashMap<>();
        c.put("text", "摘要内容");
        c.put("emb", new float[]{1f, 0f});
        chunks.add(c);
        Mockito.when(valueOps.get("ai:session:summary:vec:s1"))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(chunks));
        Mockito.when(deepSeekClient.embedding("新问题")).thenReturn(null);
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn("长期记忆内容");
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(null);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(4000);

        List<Map<String, Object>> messages = sessionService.buildMessages("s1", "系统提示", "新问题");

        assertEquals(3, messages.size(), "system+长期记忆+user");
        assertTrue(String.valueOf(messages.get(1).get("content")).contains("长期记忆内容"),
                "embedding null 应回退长期记忆");
    }

    // Test scenario: saveSummary 在 embedding 可用但返回 null 时不保存向量块
    @Test
    public void testSaveSummarySkipsEmbeddingChunkWhenReturnsNull() {
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn(null);
        Mockito.when(configService.getInt("session", "max_summary_length", 2000)).thenReturn(2000);
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        Mockito.when(deepSeekClient.embedding("新摘要")).thenReturn(null);

        sessionService.saveSummary("s1", "新摘要");

        // embedding 返回 null 时不应写入向量块 key
        Mockito.verify(valueOps, Mockito.never()).set(eq("ai:session:summary:vec:s1"), anyString());
        // 但仍应写入摘要本身
        Mockito.verify(valueOps).set(eq("ai:session:summary:s1"), anyString());
    }

    // Test scenario: getRelevantSummaryContext 向量块解析异常时回退长期记忆
    @Test
    public void testBuildMessagesFallsBackToLongTermMemoryOnVectorParseError() {
        Mockito.when(deepSeekClient.isEmbeddingAvailable()).thenReturn(true);
        Mockito.when(valueOps.get("ai:session:summary:vec:s1")).thenReturn("not-valid-json");
        Mockito.when(valueOps.get("ai:session:summary:s1")).thenReturn("异常回退记忆");
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        Mockito.when(listOps.range("ai:session:s1", 0, -1)).thenReturn(null);
        Mockito.when(configService.getInt("session", "short_term_keep", 10)).thenReturn(10);
        Mockito.when(configService.getInt("session", "max_context_tokens", 4000)).thenReturn(4000);

        List<Map<String, Object>> messages = sessionService.buildMessages("s1", "系统提示", "新问题");

        assertEquals(3, messages.size(), "system+长期记忆+user");
        assertTrue(String.valueOf(messages.get(1).get("content")).contains("异常回退记忆"),
                "向量块解析异常应回退长期记忆");
    }

    // Test scenario: extractAndSavePreferences 偏好去重后最多保留 10 个
    @Test
    public void testExtractAndSavePreferencesDedupAndLimitToTen() {
        Mockito.when(valueOps.get("ai:session:prefs:s1")).thenReturn(null);
        // 命中多个类别和价格关键词
        sessionService.extractAndSavePreferences("s1",
                "二手书教材电子产品电脑手机服装自行车宿舍用品文具运动器材便宜免费");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOps).set(eq("ai:session:prefs:s1"), captor.capture());
        String saved = captor.getValue();
        String[] parts = saved.split(" ");
        assertTrue(parts.length <= 10, "偏好去重后最多 10 个, 实际=" + saved);
        // 验证去重：不应有重复的偏好项
        java.util.Set<String> unique = new java.util.HashSet<>(java.util.Arrays.asList(parts));
        assertEquals(parts.length, unique.size(), "不应有重复偏好, 实际=" + saved);
    }
}