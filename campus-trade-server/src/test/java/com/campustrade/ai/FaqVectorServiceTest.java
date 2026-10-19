package com.campustrade.ai;

import com.campustrade.service.ai.DeepSeekClient;
import com.campustrade.service.ai.FaqVectorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

/**
 * FaqVectorService 单元测试。
 * 测试目标：FAQ 直接命中判定 findDirectFaqAnswer 的前置过滤（null/短查询/寒暄/追问类）
 * 与向量匹配逻辑（embedding 路径、TF-IDF 路径、相似度阈值），以及 buildContext、
 * search、hasRelevantFaq、rankBySimilarity、FAQ 增删改的语料与持久化行为。
 * 依赖策略：StringRedisTemplate/JdbcTemplate/DeepSeekClient 全部 Mockito mock 后反射注入，
 * FAQ 语料通过反射写入内部集合，TF-IDF 向量复用被测类自身算法构造以保证口径一致。
 */
public class FaqVectorServiceTest {

    private FaqVectorService service;
    private StringRedisTemplate redisTemplate;
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
    private JdbcTemplate jdbcTemplate;
    private DeepSeekClient deepSeekClient;

    @BeforeEach
    public void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        deepSeekClient = Mockito.mock(DeepSeekClient.class);
        // rebuildVectors 会尝试重建 embedding，打桩返回空列表使其跳过（数量不匹配）
        Mockito.when(deepSeekClient.embeddings(anyList())).thenReturn(new ArrayList<>());

        service = new FaqVectorService();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(service, "deepSeekClient", deepSeekClient);
        // @Value 字段在无容器环境下默认值为 0，需手动设置为生产默认值
        ReflectionTestUtils.setField(service, "topK", 3);
        ReflectionTestUtils.setField(service, "rerankCandidates", 8);
    }

    // ===== 内部集合读取辅助（final 集合只读取后追加，避免反射写 final 字段） =====

    @SuppressWarnings("unchecked")
    private List<FaqVectorService.FaqItem> faqItems() {
        return (List<FaqVectorService.FaqItem>) ReflectionTestUtils.getField(service, "faqItems");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Double>> faqVectors() {
        return (List<Map<String, Double>>) ReflectionTestUtils.getField(service, "faqVectors");
    }

    @SuppressWarnings("unchecked")
    private List<float[]> faqEmbeddings() {
        return (List<float[]>) ReflectionTestUtils.getField(service, "faqEmbeddings");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> idfMap() {
        return (Map<String, Double>) ReflectionTestUtils.getField(service, "idfMap");
    }

    /** 注入一条 FAQ 语料，每次注入后用被测类自身算法重建全部 IDF 与向量，保证口径一致 */
    private void injectFaqCorpus(String question, String answer) {
        FaqVectorService.FaqItem item = new FaqVectorService.FaqItem(question, answer, "交易");
        faqItems().add(item);
        faqVectors().clear();
        ReflectionTestUtils.invokeMethod(service, "computeIdf");
        for (FaqVectorService.FaqItem it : faqItems()) {
            faqVectors().add(ReflectionTestUtils.invokeMethod(service, "computeTfIdfVector", it.question));
        }
    }

    // ===== findDirectFaqAnswer：前置过滤 =====

    // Test scenario: null 查询直接返回 null
    @Test
    public void testFindDirectFaqAnswerReturnsNullForNullQuery() {
        assertNull(service.findDirectFaqAnswer(null));
    }

    // Test scenario: trim 后长度小于 4 的查询返回 null
    @Test
    public void testFindDirectFaqAnswerReturnsNullForShortQuery() {
        assertNull(service.findDirectFaqAnswer("hi"));
        assertNull(service.findDirectFaqAnswer(" 你好 "));
    }

    // Test scenario: 寒暄词命中正则返回 null
    @Test
    public void testFindDirectFaqAnswerReturnsNullForGreeting() {
        assertNull(service.findDirectFaqAnswer("你好"));
        assertNull(service.findDirectFaqAnswer("您好！"));
        assertNull(service.findDirectFaqAnswer("拜拜"));
    }

    // Test scenario: 追问类短查询（<12字符且含追问词）返回 null
    @Test
    public void testFindDirectFaqAnswerReturnsNullForFollowUpQuery() {
        assertNull(service.findDirectFaqAnswer("还有呢"));
        assertNull(service.findDirectFaqAnswer("除此之外还会啥"));
        assertNull(service.findDirectFaqAnswer("然后呢"));
        assertNull(service.findDirectFaqAnswer("更多细节"));
    }

    // ===== findDirectFaqAnswer：向量匹配 =====

    // Test scenario: embedding 路径——查询向量与 FAQ 向量一致时相似度 1.0 命中并回填分数
    @Test
    public void testFindDirectFaqAnswerMatchesViaEmbeddingPath() {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        Mockito.when(deepSeekClient.getCurrentEmbModel()).thenReturn("test-emb-model");
        Mockito.when(deepSeekClient.embedding("怎么发布商品")).thenReturn(new float[]{1f, 0f});
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");
        faqEmbeddings().add(new float[]{1f, 0f});

        FaqVectorService.FaqItem result = service.findDirectFaqAnswer("怎么发布商品");

        assertNotNull(result, "相似度 1.0 应命中阈值 0.88");
        assertEquals("怎么发布商品", result.question);
        assertEquals(1.0, result.score, 1e-6, "命中后应回填相似度分数");
    }

    // Test scenario: embedding 与 TF-IDF 相似度均低于阈值时返回 null
    @Test
    public void testFindDirectFaqAnswerReturnsNullWhenSimilarityBelowThreshold() {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        Mockito.when(deepSeekClient.getCurrentEmbModel()).thenReturn("test-emb-model");
        // embedding 未命中（返回 null），TF-IDF 查询与语料无共享 bigram，相似度为 0
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");

        FaqVectorService.FaqItem result = service.findDirectFaqAnswer("有什么活动");

        assertNull(result, "相似度低于阈值不应命中");
    }

    // Test scenario: TF-IDF 路径匹配——查询与 FAQ 语料完全一致时相似度 1.0 命中
    @Test
    public void testFindDirectFaqAnswerMatchesViaTfIdfPath() {
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");

        FaqVectorService.FaqItem result = service.findDirectFaqAnswer("怎么发布商品");

        assertNotNull(result, "TF-IDF 相似度 1.0 应命中阈值");
        assertEquals("怎么发布商品", result.question);
        assertEquals(1.0, result.score, 1e-6);
    }

    // Test scenario: 无语料时任何查询都返回 null
    @Test
    public void testFindDirectFaqAnswerReturnsNullWithEmptyCorpus() {
        assertNull(service.findDirectFaqAnswer("怎么发布商品"));
    }

    // ===== buildContext =====

    // Test scenario: null 或 trim 后短查询返回空上下文
    @Test
    public void testBuildContextReturnsEmptyForNullOrShortQuery() {
        assertEquals("", service.buildContext(null));
        assertEquals("", service.buildContext("hi"));
        assertEquals("", service.buildContext(" 你好 "));
    }

    // Test scenario: 寒暄词返回空上下文
    @Test
    public void testBuildContextReturnsEmptyForGreeting() {
        assertEquals("", service.buildContext("你好"));
        assertEquals("", service.buildContext("您好！"));
    }

    // Test scenario: 无匹配 FAQ 时返回空上下文
    @Test
    public void testBuildContextReturnsEmptyWhenNoFaqMatches() {
        assertEquals("", service.buildContext("怎么发布商品"));
    }

    // Test scenario: 匹配到 FAQ 时构建包含参考信息与回答指引的完整上下文
    @Test
    public void testBuildContextContainsFaqReference() {
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");
        String context = service.buildContext("怎么发布商品");
        assertTrue(context.startsWith("以下是与用户问题相关的常见问答参考信息"), "应以参考信息前缀开头");
        assertTrue(context.contains("怎么发布商品"), "应包含命中问题");
        assertTrue(context.contains("进入发布页填写商品信息后提交即可"), "应包含答案");
        assertTrue(context.contains("参考1"), "应包含参考编号");
        assertTrue(context.endsWith("请基于以上参考信息回答用户的问题。如果参考信息足以回答，请直接给出答案；如果用户的问题超出参考信息范围，请根据校园交易平台常识回答，并提醒用户可以联系人工客服。\n"), "应以回答指引结尾");
    }

    // ===== search / hasRelevantFaq / getAllFaqs =====

    // Test scenario: 无语料时 search 返回空列表
    @Test
    public void testSearchReturnsEmptyWhenNoCorpus() {
        assertTrue(service.search("怎么发布商品", 3).isEmpty());
    }

    // Test scenario: search 命中语料时返回 topK 条结果
    @Test
    public void testSearchReturnsTopKMatches() {
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");
        List<FaqVectorService.FaqItem> results = service.search("怎么发布商品", 3);
        assertEquals(1, results.size());
        assertEquals("怎么发布商品", results.get(0).question);
    }

    // Test scenario: hasRelevantFaq 无语料返回 false，有匹配语料返回 true
    @Test
    public void testHasRelevantFaqChecksCorpusMatch() {
        assertFalse(service.hasRelevantFaq("怎么发布商品"));
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");
        assertTrue(service.hasRelevantFaq("怎么发布商品"));
    }

    // Test scenario: getAllFaqs 返回语料副本，修改副本不影响内部语料
    @Test
    public void testGetAllFaqsReturnsCorpusCopy() {
        assertTrue(service.getAllFaqs().isEmpty());
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");
        assertEquals(1, service.getAllFaqs().size());
        service.getAllFaqs().clear();
        assertEquals(1, service.getAllFaqs().size(), "返回副本不应影响内部语料");
    }

    // ===== rankBySimilarity =====

    // Test scenario: null/空候选或空查询时原样返回候选列表
    @Test
    public void testRankBySimilarityReturnsCandidatesAsIsForInvalidInput() {
        List<String> candidates = Arrays.asList("a", "b");
        assertSame(candidates, service.rankBySimilarity(null, candidates, s -> s, 2));
        assertSame(candidates, service.rankBySimilarity("", candidates, s -> s, 2));
        assertNull(service.<String>rankBySimilarity("怎么发布商品", null, s -> s, 2), "null 候选原样返回 null");
        assertTrue(service.<String>rankBySimilarity("怎么发布商品", Collections.<String>emptyList(), s -> s, 2).isEmpty());
    }

    // Test scenario: TF-IDF 路径按相似度降序排列，相似候选排前、无关与空文本候选排后
    @Test
    public void testRankBySimilaritySortsByTfIdfSimilarity() {
        for (String bg : new String[]{"怎么", "么发", "发布", "布商", "商品", "有什", "什么", "么活", "活动"}) {
            idfMap().put(bg, 1.0);
        }
        List<String> candidates = Arrays.asList("有什么活动", "", "怎么发布商品");
        List<String> result = service.rankBySimilarity("怎么发布商品", candidates, s -> s, 3);
        assertEquals(3, result.size());
        assertEquals("怎么发布商品", result.get(0), "相似度高的候选应排在首位");
        assertEquals("", result.get(2), "空文本候选相似度为 0 应排在末位");
    }

    // Test scenario: topK 截取返回候选数量
    @Test
    public void testRankBySimilarityLimitsToTopK() {
        for (String bg : new String[]{"怎么", "么发", "发布", "布商", "商品", "教材"}) {
            idfMap().put(bg, 1.0);
        }
        List<String> candidates = Arrays.asList("怎么发布商品", "怎么发布二手书", "怎么发布教材");
        List<String> result = service.rankBySimilarity("怎么发布商品", candidates, s -> s, 1);
        assertEquals(1, result.size());
        assertEquals("怎么发布商品", result.get(0));
    }

    // ===== addFaq / updateFaq / deleteFaq =====

    // Test scenario: addFaq 插入 DB 后回填自增 id 并加入语料、重建向量并写 Redis 缓存
    @Test
    public void testAddFaqPersistsAndRebuildsVectors() {
        Mockito.when(jdbcTemplate.update(anyString(), ArgumentMatchers.<Object>any())).thenReturn(1);
        Mockito.when(jdbcTemplate.queryForObject(contains("LAST_INSERT_ID"), eq(Long.class))).thenReturn(5L);

        FaqVectorService.FaqItem item = new FaqVectorService.FaqItem("怎么查询订单", "在订单列表页查看", "交易");
        service.addFaq(item);

        assertEquals(5L, item.id, "应回填自增 id");
        assertEquals(1, faqItems().size(), "语料应新增一条");
        assertEquals("怎么查询订单", faqItems().get(0).question);
        Mockito.verify(valueOps).set(eq("ai:faq:items"), anyString());
        Mockito.verify(valueOps).set(eq("ai:faq:idf"), anyString());
        Mockito.verify(valueOps).set(eq("ai:faq:vectors"), anyString());
    }

    // Test scenario: updateFaq 越界索引直接返回，不触碰语料与 DB
    @Test
    public void testUpdateFaqIgnoresInvalidIndex() {
        injectFaqCorpus("怎么发布商品", "发布页提交");
        FaqVectorService.FaqItem item = new FaqVectorService.FaqItem("怎么退货", "退货页提交", "交易");
        service.updateFaq(5, item);
        service.updateFaq(-1, item);
        assertEquals("怎么发布商品", faqItems().get(0).question, "越界索引不应修改语料");
        Mockito.verifyNoInteractions(jdbcTemplate);
    }

    // Test scenario: updateFaq 合法索引更新语料、保留原 id 并同步 DB
    @Test
    public void testUpdateFaqUpdatesCorpusAndKeepsId() {
        injectFaqCorpus("怎么发布商品", "发布页提交");
        faqItems().get(0).id = 7L;
        FaqVectorService.FaqItem item = new FaqVectorService.FaqItem("怎么退货退款", "退货页提交", "交易");
        service.updateFaq(0, item);

        assertEquals(7L, item.id, "更新后应保留原 id");
        assertEquals("怎么退货退款", faqItems().get(0).question, "语料应被更新");
        Mockito.verify(jdbcTemplate).update(anyString(), any(), any(), any(), eq(7L));
    }

    // Test scenario: deleteFaq 合法索引从语料移除并删除 DB 记录
    @Test
    public void testDeleteFaqRemovesFromCorpusAndDb() {
        injectFaqCorpus("怎么发布商品", "发布页提交");
        faqItems().get(0).id = 9L;
        service.deleteFaq(0);

        assertTrue(faqItems().isEmpty(), "语料应被移除");
        Mockito.verify(jdbcTemplate).update(anyString(), eq(9L));
    }

    // Test scenario: deleteFaq 越界索引不做任何操作
    @Test
    public void testDeleteFaqIgnoresInvalidIndex() {
        service.deleteFaq(0);
        Mockito.verifyNoInteractions(jdbcTemplate);
    }

    // ===== retryEmbeddings =====

    // Test scenario: embedding 关闭且语料非空时 retryEmbeddings 尝试重新初始化，
    // DeepSeekClient 返回空列表时数量不匹配，不开启 embedding 模式
    @Test
    public void testRetryEmbeddingsTriggersInitWhenDisabled() {
        injectFaqCorpus("怎么发布商品", "发布页提交");
        Mockito.when(valueOps.get(anyString())).thenReturn(null);

        service.retryEmbeddings();

        Mockito.verify(deepSeekClient).embeddings(anyList());
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(service, "useEmbeddings"),
                "embedding 数量不匹配时不应开启 embedding 模式");
    }

    // Test scenario: embedding 已开启或语料为空时 retryEmbeddings 不做任何事
    @Test
    public void testRetryEmbeddingsSkipsWhenAlreadyEnabled() {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        service.retryEmbeddings();
        Mockito.verifyNoInteractions(deepSeekClient);
    }

    // ==================== init 全链路 ====================

    // Test scenario: init Redis 无缓存且 DB 不可用时回退 faq-data.json 并写入缓存（语料数量取决于资源可用性）
    @Test
    public void testInitFallsBackToJsonSourceAndSavesCache() {
        Mockito.when(valueOps.get(anyString())).thenReturn(null);
        Mockito.when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("db down"));
        Mockito.when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> service.init());
        // 空语料也应写入 Redis 缓存（saveToRedis）
        Mockito.verify(valueOps).set(eq("ai:faq:items"), anyString());
        Mockito.verify(valueOps).set(eq("ai:faq:idf"), anyString());
        Mockito.verify(valueOps).set(eq("ai:faq:vectors"), anyString());
    }

    // Test scenario: init 从 Redis 缓存加载语料与 embedding 向量并开启 embedding 模式
    @Test
    public void testInitLoadsFromRedisCacheWithEmbeddings() throws Exception {
        List<Map<String, Double>> vectors = new ArrayList<>();
        Map<String, Double> v = new HashMap<>();
        v.put("怎么", 0.2);
        v.put("么发", 0.2);
        vectors.add(v);
        Mockito.when(valueOps.get("ai:faq:items")).thenReturn(
                "[{\"question\":\"怎么发布商品\",\"answer\":\"发布页提交\",\"category\":\"交易\",\"id\":1}]");
        Mockito.when(valueOps.get("ai:faq:idf")).thenReturn("{\"怎么\":0.5,\"么发\":0.5}");
        Mockito.when(valueOps.get("ai:faq:vectors")).thenReturn(new ObjectMapper().writeValueAsString(vectors));
        Mockito.when(valueOps.get(contains("ai:faq:embeddings:v2"))).thenReturn("[[1.0,0.0]]");

        service.init();

        assertEquals(1, service.getAllFaqs().size());
        assertEquals("怎么发布商品", service.getAllFaqs().get(0).question);
        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(service, "useEmbeddings"),
                "embedding 缓存数量匹配应开启 embedding 模式");
    }

    // Test scenario: Redis 缓存数据损坏时清空回退重新加载
    @Test
    public void testInitClearsCorruptedRedisCache() {
        Mockito.when(valueOps.get("ai:faq:items")).thenReturn("not-valid-json");
        Mockito.when(valueOps.get("ai:faq:idf")).thenReturn("{}");
        Mockito.when(valueOps.get("ai:faq:vectors")).thenReturn("[]");
        Mockito.when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("db down"));
        Mockito.when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> service.init());
        assertFalse(service.getAllFaqs().isEmpty(), "坏缓存清除且DB不可用时应从classpath json回退加载语料");
    }

    // ==================== search 的 embedding 融合与 rerank 路径 ====================

    // Test scenario: embedding 开启时 search 融合 TF-IDF 与 embedding 排序（RRF）
    @Test
    public void testSearchFusesTfIdfAndEmbeddingRanking() {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        Mockito.when(deepSeekClient.getCurrentEmbModel()).thenReturn("emb-model");
        Mockito.when(deepSeekClient.embedding("怎么发布商品")).thenReturn(new float[]{1f, 0f});
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交");
        injectFaqCorpus("怎么查询订单", "在订单列表页查看");
        faqEmbeddings().add(new float[]{1f, 0f});
        faqEmbeddings().add(new float[]{0f, 1f});

        List<FaqVectorService.FaqItem> results = service.search("怎么发布商品", 2);
        assertEquals(2, results.size());
        assertEquals("怎么发布商品", results.get(0).question, "双路得分融合后仍应排首位");
    }

    // Test scenario: rerank 启用且候选多于 3 时调用模型重排序并按评分排序
    @Test
    public void testSearchReranksWhenEnabledAndCandidatesExceedThree() {
        ReflectionTestUtils.setField(service, "rerankEnabled", true);
        Mockito.when(deepSeekClient.chat(anyList())).thenReturn("[3, 1, 2]");
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交");
        injectFaqCorpus("怎么查询订单", "在订单列表页查看");
        injectFaqCorpus("怎么退货退款", "在退货页申请");
        StringBuilder longAnswer = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            longAnswer.append("长");
        }
        injectFaqCorpus("怎么修改地址", longAnswer.toString());

        List<FaqVectorService.FaqItem> results = service.search("怎么发布商品", 3);
        Mockito.verify(deepSeekClient).chat(anyList());
        assertEquals(3, results.size(), "评分只有 3 个时只重排 3 个候选");
        assertEquals("怎么发布商品", results.get(0).question, "评分最高(3)的应排首位");
        // 重排序结果应写入缓存
        Mockito.verify(valueOps, Mockito.atLeastOnce()).set(anyString(), anyString(), eq(1L), eq(TimeUnit.HOURS));
    }

    // Test scenario: rerank 缓存命中时按缓存顺序返回且不调用模型
    @Test
    public void testSearchRerankUsesCachedOrder() {
        ReflectionTestUtils.setField(service, "rerankEnabled", true);
        Mockito.when(valueOps.get(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            if (key.startsWith("ai:rerank:cache:")) {
                return "[\"怎么发布商品\",\"怎么查询订单\",\"怎么退货退款\",\"怎么修改地址\"]";
            }
            return null;
        });
        injectFaqCorpus("怎么查询订单", "在订单列表页查看");
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交");
        injectFaqCorpus("怎么退货退款", "在退货页申请");
        injectFaqCorpus("怎么修改地址", "在地址管理页修改");

        List<FaqVectorService.FaqItem> results = service.search("怎么发布商品", 3);
        Mockito.verify(deepSeekClient, Mockito.never()).chat(anyList());
        assertEquals(3, results.size());
        assertEquals("怎么发布商品", results.get(0).question);
        assertEquals("怎么查询订单", results.get(1).question);
    }

    // Test scenario: rerank 模型返回异常内容时回退原始顺序
    @Test
    public void testSearchRerankFallsBackOnBadModelResponse() {
        ReflectionTestUtils.setField(service, "rerankEnabled", true);
        Mockito.when(deepSeekClient.chat(anyList())).thenReturn("模型返回异常无数组");
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交");
        injectFaqCorpus("怎么查询订单", "在订单列表页查看");
        injectFaqCorpus("怎么退货退款", "在退货页申请");
        injectFaqCorpus("怎么修改地址", "在地址管理页修改");

        List<FaqVectorService.FaqItem> results = service.search("怎么发布商品", 3);
        assertEquals(3, results.size(), "模型异常应回退原始顺序仍返回 topK");
        assertEquals("怎么发布商品", results.get(0).question, "回退后保持 TF-IDF 原始排序");
    }

    // ==================== rankBySimilarity 的 embedding 路径 ====================

    // Test scenario: rankBySimilarity 在 embedding 开启时按向量相似度排序并截取 topK
    @Test
    public void testRankBySimilarityUsesEmbeddingPath() {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        Mockito.when(deepSeekClient.embedding("怎么发布商品")).thenReturn(new float[]{1f, 0f});
        Mockito.when(deepSeekClient.embeddings(anyList()))
                .thenReturn(Arrays.asList(new float[]{1f, 0f}, new float[]{0f, 1f}));
        List<String> candidates = Arrays.asList("怎么发布商品", "无关文本xyz");
        List<String> result = service.rankBySimilarity("怎么发布商品", candidates, s -> s, 1);
        assertEquals(1, result.size());
        assertEquals("怎么发布商品", result.get(0));
    }

    // ==================== addFaq 异常路径 ====================

    // Test scenario: addFaq 数据库异常时仍加入语料并重建向量写入缓存
    @Test
    public void testAddFaqSwallowsDbException() {
        Mockito.when(jdbcTemplate.update(anyString(), ArgumentMatchers.<Object>any()))
                .thenThrow(new RuntimeException("db down"));
        Mockito.when(jdbcTemplate.queryForObject(contains("LAST_INSERT_ID"), eq(Long.class)))
                .thenThrow(new RuntimeException("db down"));
        FaqVectorService.FaqItem item = new FaqVectorService.FaqItem("怎么查询订单", "在订单列表页查看", "交易");
        service.addFaq(item);
        assertEquals(1, faqItems().size(), "DB 异常时语料仍应新增");
        assertNull(item.id, "id 回填失败保持 null");
        Mockito.verify(valueOps).set(eq("ai:faq:items"), anyString());
    }

    // ==================== embedding 缓存命中路径 ====================

    // Test scenario: search 在 embedding 缓存命中时不调用 embedding API
    @Test
    public void testSearchUsesEmbeddingCache() throws Exception {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        Mockito.when(deepSeekClient.getCurrentEmbModel()).thenReturn("emb-model");
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交");
        faqEmbeddings().add(new float[]{1f, 0f});
        // 缓存命中：emb cache key 返回预计算的 embedding
        Mockito.when(valueOps.get(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            if (key.startsWith("ai:emb:cache:")) {
                return new ObjectMapper().writeValueAsString(new float[]{1f, 0f});
            }
            return null;
        });

        List<FaqVectorService.FaqItem> results = service.search("怎么发布商品", 1);

        assertEquals(1, results.size());
        assertEquals("怎么发布商品", results.get(0).question);
        Mockito.verify(deepSeekClient, Mockito.never()).embedding(anyString());
    }

    // Test scenario: findDirectFaqAnswer 在 embedding 缓存命中时不调用 embedding API
    @Test
    public void testFindDirectFaqAnswerUsesEmbeddingCache() throws Exception {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        Mockito.when(deepSeekClient.getCurrentEmbModel()).thenReturn("emb-model");
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");
        faqEmbeddings().add(new float[]{1f, 0f});
        Mockito.when(valueOps.get(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            if (key.startsWith("ai:emb:cache:")) {
                return new ObjectMapper().writeValueAsString(new float[]{1f, 0f});
            }
            return null;
        });

        FaqVectorService.FaqItem result = service.findDirectFaqAnswer("怎么发布商品");

        assertNotNull(result, "缓存命中应通过 embedding 路径匹配");
        assertEquals("怎么发布商品", result.question);
        assertEquals(1.0, result.score, 1e-6, "完全匹配相似度应为 1.0");
        Mockito.verify(deepSeekClient, Mockito.never()).embedding(anyString());
    }

    // Test scenario: findDirectFaqAnswer 短查询（<8字符）使用更高阈值 0.88
    @Test
    public void testFindDirectFaqAnswerUsesHigherThresholdForShortQuery() {
        // "怎么发布商品" 6 字符 < 8，阈值 0.88；TF-IDF 完全匹配相似度 1.0 >= 0.88 应命中
        injectFaqCorpus("怎么发布商品", "进入发布页填写商品信息后提交即可");
        FaqVectorService.FaqItem result = service.findDirectFaqAnswer("怎么发布商品");
        assertNotNull(result, "短查询完全匹配应命中高阈值 0.88");
        assertEquals("怎么发布商品", result.question);
    }

    // Test scenario: search topK 大于语料数量时返回全部语料
    @Test
    public void testSearchReturnsAllWhenTopKExceedsCorpus() {
        injectFaqCorpus("怎么发布商品", "发布页提交");
        injectFaqCorpus("怎么查询订单", "订单列表查看");
        List<FaqVectorService.FaqItem> results = service.search("怎么发布商品", 10);
        assertEquals(2, results.size(), "topK 超过语料数应返回全部");
    }

    // Test scenario: buildContext 对追问类短查询返回空上下文
    @Test
    public void testBuildContextReturnsEmptyForFollowUpQuery() {
        assertEquals("", service.buildContext("还有呢"));
        assertEquals("", service.buildContext("然后呢"));
    }

    // Test scenario: rankBySimilarity 在 embedding 路径候选数量不匹配时回退 TF-IDF
    @Test
    public void testRankBySimilarityFallsBackToTfIdfWhenEmbCountMismatch() {
        ReflectionTestUtils.setField(service, "useEmbeddings", true);
        Mockito.when(deepSeekClient.embedding("怎么发布商品")).thenReturn(new float[]{1f, 0f});
        // embeddings 返回数量不匹配（1 个而非 2 个），应回退 TF-IDF
        Mockito.when(deepSeekClient.embeddings(anyList()))
                .thenReturn(Collections.singletonList(new float[]{1f, 0f}));
        for (String bg : new String[]{"怎么", "么发", "发布", "布商", "商品", "有什", "什么", "么活", "活动"}) {
            idfMap().put(bg, 1.0);
        }
        List<String> candidates = Arrays.asList("有什么活动", "怎么发布商品");
        List<String> result = service.rankBySimilarity("怎么发布商品", candidates, s -> s, 2);
        assertEquals(2, result.size(), "回退 TF-IDF 应返回全部候选");
        assertEquals("怎么发布商品", result.get(0), "TF-IDF 相似度高的应排首位");
    }
}