package com.campustrade.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FaqVectorService {

    @Value("${ai.faq.tfidf-threshold:0.15}")
    private double similarityThreshold;

    @Value("${ai.faq.embedding-threshold:0.25}")
    private double embeddingThreshold;

    @Value("${ai.faq.top-k:3}")
    private int topK;

    @Value("${ai.faq.rerank.enabled:true}")
    private boolean rerankEnabled;

    @Value("${ai.faq.rerank.candidates:8}")
    private int rerankCandidates;

    private final List<FaqItem> faqItems = new CopyOnWriteArrayList<>();
    private final List<Map<String, Double>> faqVectors = new CopyOnWriteArrayList<>();
    private final List<float[]> faqEmbeddings = new CopyOnWriteArrayList<>();
    private final Map<String, Double> idfMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean useEmbeddings = false;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private DeepSeekClient deepSeekClient;

    private static final String REDIS_KEY_ITEMS = "ai:faq:items";
    private static final String REDIS_KEY_IDF = "ai:faq:idf";
    private static final String REDIS_KEY_VECTORS = "ai:faq:vectors";
    private String getEmbeddingsCacheKey() {
        String model = deepSeekClient != null ? deepSeekClient.getCurrentEmbModel() : "default";
        return "ai:faq:embeddings:v2:" + model;
    }

    public static class FaqItem {
        public Long id;
        public String question;
        public String answer;
        public String category;

        public FaqItem() {}

        public FaqItem(String question, String answer, String category) {
            this.question = question;
            this.answer = answer;
            this.category = category;
        }
    }

    @PostConstruct
    public void init() {
        if (!loadFromRedis()) {
            loadFaqData();
            computeIdf();
            for (FaqItem item : faqItems) {
                faqVectors.add(computeTfIdfVector(item.question));
            }
            saveToRedis();
            log.info("FaqVectorService initialized: {} FAQ items loaded", faqItems.size());
        } else {
            log.info("FaqVectorService initialized from Redis cache: {} FAQ items loaded", faqItems.size());
        }
        tryInitEmbeddings();
        syncToDatabaseIfEmpty();
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000)
    public void retryEmbeddings() {
        if (!useEmbeddings && !faqItems.isEmpty() && deepSeekClient != null) {
            log.info("Retrying embedding initialization...");
            tryInitEmbeddings();
        }
    }

    private void tryInitEmbeddings() {
        try {
            String embeddingsJson = stringRedisTemplate.opsForValue().get(getEmbeddingsCacheKey());
            if (embeddingsJson != null && !embeddingsJson.isEmpty()) {
                List<float[]> cached = objectMapper.readValue(embeddingsJson, new TypeReference<List<float[]>>() {});
                if (cached.size() == faqItems.size() && !cached.isEmpty()) {
                    faqEmbeddings.addAll(cached);
                    useEmbeddings = true;
                    log.info("FAQ embeddings loaded from Redis cache: {} vectors", faqEmbeddings.size());
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load embeddings from Redis: {}", e.getMessage());
        }
        if (deepSeekClient != null) {
            try {
                List<String> questions = new ArrayList<>();
                for (FaqItem item : faqItems) {
                    questions.add(item.question);
                }
                List<float[]> embeddings = deepSeekClient.embeddings(questions);
                if (embeddings.size() == faqItems.size() && !embeddings.isEmpty()) {
                    faqEmbeddings.addAll(embeddings);
                    useEmbeddings = true;
                    try {
                        stringRedisTemplate.opsForValue().set(getEmbeddingsCacheKey(), objectMapper.writeValueAsString(faqEmbeddings));
                    } catch (Exception ignored) {}
                    log.info("FAQ embeddings computed and cached: {} vectors, dim={}", faqEmbeddings.size(), faqEmbeddings.get(0).length);
                } else {
                    log.info("Embedding API returned insufficient results, falling back to TF-IDF");
                }
            } catch (Exception e) {
                log.warn("Failed to compute embeddings, using TF-IDF fallback: {}", e.getMessage());
            }
        }
    }

    private void syncToDatabaseIfEmpty() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_faq", Long.class);
            if (count != null && count == 0 && !faqItems.isEmpty()) {
                for (FaqItem item : faqItems) {
                    try {
                        jdbcTemplate.update("INSERT INTO t_faq (question, answer, category) VALUES (?, ?, ?)",
                            item.question, item.answer, item.category);
                    } catch (Exception ignored) {}
                }
                log.info("Synced {} FAQs to database", faqItems.size());
            }
        } catch (Exception e) {
            log.warn("Failed to sync FAQs to database: {}", e.getMessage());
        }
    }

    private boolean loadFromRedis() {
        try {
            String itemsJson = stringRedisTemplate.opsForValue().get(REDIS_KEY_ITEMS);
            String idfJson = stringRedisTemplate.opsForValue().get(REDIS_KEY_IDF);
            String vectorsJson = stringRedisTemplate.opsForValue().get(REDIS_KEY_VECTORS);
            if (itemsJson == null || idfJson == null || vectorsJson == null) return false;
            faqItems.addAll(objectMapper.readValue(itemsJson, new TypeReference<List<FaqItem>>() {}));
            idfMap.putAll(objectMapper.readValue(idfJson, new TypeReference<Map<String, Double>>() {}));
            faqVectors.addAll(objectMapper.readValue(vectorsJson, new TypeReference<List<Map<String, Double>>>() {}));
            return !faqItems.isEmpty();
        } catch (Exception e) {
            log.warn("Failed to load FAQ cache from Redis, will recompute: {}", e.getMessage());
            faqItems.clear();
            idfMap.clear();
            faqVectors.clear();
            return false;
        }
    }

    private void saveToRedis() {
        try {
            stringRedisTemplate.opsForValue().set(REDIS_KEY_ITEMS, objectMapper.writeValueAsString(faqItems));
            stringRedisTemplate.opsForValue().set(REDIS_KEY_IDF, objectMapper.writeValueAsString(idfMap));
            stringRedisTemplate.opsForValue().set(REDIS_KEY_VECTORS, objectMapper.writeValueAsString(faqVectors));
        } catch (Exception e) {
            log.warn("Failed to save FAQ cache to Redis: {}", e.getMessage());
        }
    }

    public List<FaqItem> getAllFaqs() {
        return new ArrayList<>(faqItems);
    }

    public void addFaq(FaqItem item) {
        try {
            jdbcTemplate.update("INSERT INTO t_faq (question, answer, category) VALUES (?, ?, ?)",
                item.question, item.answer, item.category);
            item.id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } catch (Exception e) {
            log.warn("Failed to insert FAQ to database: {}", e.getMessage());
        }
        faqItems.add(item);
        rebuildVectors();
    }

    public void updateFaq(int index, FaqItem item) {
        if (index < 0 || index >= faqItems.size()) return;
        FaqItem existing = faqItems.get(index);
        try {
            jdbcTemplate.update("UPDATE t_faq SET question=?, answer=?, category=? WHERE id=?",
                item.question, item.answer, item.category, existing.id);
        } catch (Exception e) {
            log.warn("Failed to update FAQ in database: {}", e.getMessage());
        }
        item.id = existing.id;
        faqItems.set(index, item);
        rebuildVectors();
    }

    public void deleteFaq(int index) {
        if (index < 0 || index >= faqItems.size()) return;
        FaqItem existing = faqItems.get(index);
        try {
            jdbcTemplate.update("DELETE FROM t_faq WHERE id=?", existing.id);
        } catch (Exception e) {
            log.warn("Failed to delete FAQ from database: {}", e.getMessage());
        }
        faqItems.remove(index);
        rebuildVectors();
    }

    private void rebuildVectors() {
        computeIdf();
        List<Map<String, Double>> newVectors = new ArrayList<>();
        for (FaqItem item : faqItems) {
            newVectors.add(computeTfIdfVector(item.question));
        }
        faqVectors.clear();
        faqVectors.addAll(newVectors);
        saveToRedis();
        rebuildEmbeddings();
        log.info("FAQ vectors rebuilt: {} items, embeddings: {}", faqItems.size(), useEmbeddings ? "on" : "off");
    }

    private void rebuildEmbeddings() {
        if (deepSeekClient == null || faqItems.isEmpty()) return;
        try {
            List<String> questions = new ArrayList<>();
            for (FaqItem item : faqItems) {
                questions.add(item.question);
            }
            List<float[]> embeddings = deepSeekClient.embeddings(questions);
            if (embeddings.size() == faqItems.size()) {
                faqEmbeddings.clear();
                faqEmbeddings.addAll(embeddings);
                useEmbeddings = true;
                try {
                    stringRedisTemplate.opsForValue().set(getEmbeddingsCacheKey(), objectMapper.writeValueAsString(faqEmbeddings));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Failed to rebuild embeddings: {}", e.getMessage());
        }
    }


    private void loadFaqData() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, question, answer, category FROM t_faq ORDER BY sort_order, id");
            if (!rows.isEmpty()) {
                for (Map<String, Object> row : rows) {
                    FaqItem item = new FaqItem();
                    item.id = ((Number) row.get("id")).longValue();
                    item.question = (String) row.get("question");
                    item.answer = (String) row.get("answer");
                    item.category = (String) row.get("category");
                    faqItems.add(item);
                }
                log.info("Loaded {} FAQs from database", faqItems.size());
                return;
            }
        } catch (Exception e) {
            log.warn("Failed to load FAQ from database: {}", e.getMessage());
        }
        try (InputStream is = new ClassPathResource("faq-data.json").getInputStream()) {
            List<FaqItem> loaded = objectMapper.readValue(is, new TypeReference<List<FaqItem>>() {});
            for (FaqItem item : loaded) {
                try {
                    jdbcTemplate.update("INSERT INTO t_faq (question, answer, category) VALUES (?, ?, ?)",
                        item.question, item.answer, item.category);
                } catch (Exception ignored) {}
            }
            faqItems.addAll(loaded);
            log.info("Loaded {} FAQs from json and inserted into database", loaded.size());
        } catch (Exception e) {
            log.error("Failed to load faq-data.json", e);
        }
    }

    private Set<String> extractBigrams(String text) {
        Set<String> bigrams = new HashSet<>();
        if (text == null || text.length() < 2) return bigrams;
        String cleaned = text.replaceAll("[\\s\\p{Punct}]+", "").toLowerCase();
        for (int i = 0; i < cleaned.length() - 1; i++) {
            bigrams.add(cleaned.substring(i, i + 2));
        }
        return bigrams;
    }

    private Map<String, Double> computeTfVector(String text) {
        Map<String, Double> tf = new HashMap<>();
        Set<String> bigrams = extractBigrams(text);
        for (String bigram : bigrams) {
            tf.merge(bigram, 1.0, Double::sum);
        }
        int total = bigrams.size();
        if (total > 0) {
            for (String key : tf.keySet()) {
                tf.put(key, tf.get(key) / total);
            }
        }
        return tf;
    }

    private void computeIdf() {
        Map<String, Integer> docFreq = new HashMap<>();
        int totalDocs = faqItems.size();
        for (FaqItem item : faqItems) {
            Set<String> bigrams = extractBigrams(item.question);
            for (String bigram : bigrams) {
                docFreq.merge(bigram, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            idfMap.put(entry.getKey(), Math.log((double) totalDocs / (entry.getValue() + 1)));
        }
    }

    private Map<String, Double> computeTfIdfVector(String text) {
        Map<String, Double> tf = computeTfVector(text);
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            double idf = idfMap.getOrDefault(entry.getKey(), 0.0);
            tfidf.put(entry.getKey(), entry.getValue() * idf);
        }
        return tfidf;
    }

    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        if (v1.isEmpty() || v2.isEmpty()) return 0.0;
        double dotProduct = 0.0;
        for (String key : v1.keySet()) {
            if (v2.containsKey(key)) {
                dotProduct += v1.get(key) * v2.get(key);
            }
        }
        double norm1 = Math.sqrt(v1.values().stream().mapToDouble(x -> x * x).sum());
        double norm2 = Math.sqrt(v2.values().stream().mapToDouble(x -> x * x).sum());
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (norm1 * norm2);
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length || v1.length == 0) return 0.0;
        double dotProduct = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private List<FaqItem> rerankResults(String query, List<FaqItem> candidates) {
        if (candidates.size() <= 3) return candidates;
        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("对以下FAQ与用户问题的相关性打分(0-10)，只输出JSON数组：\n");
            promptBuilder.append("问题：").append(query).append("\n候选：\n");
            for (int i = 0; i < candidates.size(); i++) {
                FaqItem item = candidates.get(i);
                String aSummary = item.answer.length() > 100 ? item.answer.substring(0, 100) : item.answer;
                promptBuilder.append(i + 1).append(". ").append(item.question).append("：").append(aSummary).append("\n");
            }
            promptBuilder.append("输出格式：[8.5, 3.2, 7.1, ...]");
            List<Map<String, Object>> msgs = new ArrayList<>();
            Map<String, Object> m = new HashMap<>();
            m.put("role", "user");
            m.put("content", promptBuilder.toString());
            msgs.add(m);
            String response = deepSeekClient.chat(msgs);
            if (response == null || response.isEmpty()) return candidates;
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start < 0 || end < 0) return candidates;
            JSONArray scores = JSONUtil.parseArray(response.substring(start, end + 1));
            List<Map.Entry<FaqItem, Double>> reranked = new ArrayList<>();
            for (int i = 0; i < candidates.size() && i < scores.size(); i++) {
                reranked.add(new AbstractMap.SimpleEntry<>(candidates.get(i), scores.getDouble(i)));
            }
            reranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            List<FaqItem> result = new ArrayList<>();
            for (Map.Entry<FaqItem, Double> entry : reranked) {
                result.add(entry.getKey());
            }
            log.info("Reranked {} FAQ candidates for query: {}", candidates.size(), query.substring(0, Math.min(30, query.length())));
            return result;
        } catch (Exception e) {
            log.warn("Rerank failed, using original order: {}", e.getMessage());
            return candidates;
        }
    }

    public List<FaqItem> search(String query, int topK) {
        List<Map.Entry<FaqItem, Double>> tfidfScored = new ArrayList<>();
        Map<String, Double> queryVector = computeTfIdfVector(query);
        for (int i = 0; i < faqItems.size(); i++) {
            double score = cosineSimilarity(queryVector, faqVectors.get(i));
            tfidfScored.add(new AbstractMap.SimpleEntry<>(faqItems.get(i), score));
        }
        tfidfScored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Map.Entry<FaqItem, Double>> embScored = null;
        if (useEmbeddings && !faqEmbeddings.isEmpty() && deepSeekClient != null) {
            float[] queryEmbedding = null;
            String embModel = deepSeekClient.getCurrentEmbModel();
            String embCacheKey = "ai:emb:cache:" + embModel + ":" + Math.abs(query.hashCode());
            try {
                String cached = stringRedisTemplate.opsForValue().get(embCacheKey);
                if (cached != null && !cached.isEmpty()) {
                    queryEmbedding = objectMapper.readValue(cached, float[].class);
                }
            } catch (Exception ignored) {}
            if (queryEmbedding == null) {
                queryEmbedding = deepSeekClient.embedding(query);
                if (queryEmbedding != null && queryEmbedding.length > 0) {
                    try {
                        stringRedisTemplate.opsForValue().set(embCacheKey, objectMapper.writeValueAsString(queryEmbedding), 1, java.util.concurrent.TimeUnit.HOURS);
                    } catch (Exception ignored) {}
                }
            }
            if (queryEmbedding != null && queryEmbedding.length > 0) {
                embScored = new ArrayList<>();
                for (int i = 0; i < faqItems.size(); i++) {
                    double score = cosineSimilarity(queryEmbedding, faqEmbeddings.get(i));
                    embScored.add(new AbstractMap.SimpleEntry<>(faqItems.get(i), score));
                }
                embScored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            }
        }

        int rrfK = 60;
        Map<FaqItem, Double> rrfScores = new LinkedHashMap<>();
        for (int i = 0; i < tfidfScored.size(); i++) {
            rrfScores.merge(tfidfScored.get(i).getKey(), 1.0 / (rrfK + i + 1), Double::sum);
        }
        if (embScored != null) {
            for (int i = 0; i < embScored.size(); i++) {
                rrfScores.merge(embScored.get(i).getKey(), 1.0 / (rrfK + i + 1), Double::sum);
            }
        }
        List<Map.Entry<FaqItem, Double>> fused = new ArrayList<>(rrfScores.entrySet());
        fused.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<FaqItem> candidates = new ArrayList<>();
        for (int i = 0; i < Math.min(rerankCandidates, fused.size()); i++) {
            candidates.add(fused.get(i).getKey());
        }
        if (rerankEnabled && candidates.size() > 3) {
            candidates = rerankResults(query, candidates);
        }
        List<FaqItem> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, candidates.size()); i++) {
            results.add(candidates.get(i));
        }
        return results;
    }

    public String buildContext(String query) {
        if (query == null || query.trim().length() < 4) return "";
        String lower = query.toLowerCase().trim();
        if (lower.matches("^(你好|您好|谢谢|感谢|再见|拜拜|晚安|早安|好的|嗯|ok|bye|hi|hello|嗨|哈喽|hey)[啊呀！!。.~]?$")) return "";
        List<FaqItem> matches = search(query, topK);
        if (matches.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder("以下是与用户问题相关的常见问答参考信息：\n\n");
        for (int i = 0; i < matches.size(); i++) {
            FaqItem item = matches.get(i);
            context.append("参考").append(i + 1).append("：\n问题：")
                   .append(item.question).append("\n答案：").append(item.answer).append("\n\n");
        }
        context.append("请基于以上参考信息回答用户的问题。如果参考信息足以回答，请直接给出答案；如果用户的问题超出参考信息范围，请根据校园交易平台常识回答，并提醒用户可以联系人工客服。\n");
        return context.toString();
    }

    public boolean hasRelevantFaq(String query) {
        return !search(query, 1).isEmpty();
    }

    public <T> List<T> rankBySimilarity(String query, List<T> candidates, java.util.function.Function<T, String> textExtractor, int topK) {
        if (query == null || query.trim().isEmpty() || candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        if (useEmbeddings && deepSeekClient != null) {
            float[] queryEmbedding = deepSeekClient.embedding(query);
            if (queryEmbedding != null && queryEmbedding.length > 0) {
                List<String> texts = new ArrayList<>();
                for (T candidate : candidates) {
                    String text = textExtractor.apply(candidate);
                    texts.add(text != null ? text : "");
                }
                List<float[]> candidateEmbeddings = deepSeekClient.embeddings(texts);
                if (candidateEmbeddings.size() == candidates.size()) {
                    List<Map.Entry<T, Double>> scored = new ArrayList<>();
                    for (int i = 0; i < candidates.size(); i++) {
                        double score = cosineSimilarity(queryEmbedding, candidateEmbeddings.get(i));
                        scored.add(new AbstractMap.SimpleEntry<>(candidates.get(i), score));
                    }
                    scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                    List<T> result = new ArrayList<>();
                    for (int i = 0; i < Math.min(topK, scored.size()); i++) {
                        result.add(scored.get(i).getKey());
                    }
                    return result;
                }
            }
        }
        Map<String, Double> queryVector = computeTfIdfVector(query);
        List<Map.Entry<T, Double>> scored = new ArrayList<>();
        for (T candidate : candidates) {
            String text = textExtractor.apply(candidate);
            if (text != null && !text.isEmpty()) {
                double score = cosineSimilarity(queryVector, computeTfIdfVector(text));
                scored.add(new AbstractMap.SimpleEntry<>(candidate, score));
            } else {
                scored.add(new AbstractMap.SimpleEntry<>(candidate, 0.0));
            }
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<T> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            result.add(scored.get(i).getKey());
        }
        return result;
    }
}