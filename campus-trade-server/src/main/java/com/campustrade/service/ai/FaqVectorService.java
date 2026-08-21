package com.campustrade.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class FaqVectorService {

    private static final double SIMILARITY_THRESHOLD = 0.15;
    private static final int TOP_K = 3;

    private final List<FaqItem> faqItems = new ArrayList<>();
    private final List<Map<String, Double>> faqVectors = new ArrayList<>();
    private final Map<String, Double> idfMap = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_KEY_ITEMS = "ai:faq:items";
    private static final String REDIS_KEY_IDF = "ai:faq:idf";
    private static final String REDIS_KEY_VECTORS = "ai:faq:vectors";

    public static class FaqItem {
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
        faqItems.add(item);
        rebuildVectors();
    }

    public void updateFaq(int index, FaqItem item) {
        if (index < 0 || index >= faqItems.size()) return;
        faqItems.set(index, item);
        rebuildVectors();
    }

    public void deleteFaq(int index) {
        if (index < 0 || index >= faqItems.size()) return;
        faqItems.remove(index);
        rebuildVectors();
    }

    private void rebuildVectors() {
        computeIdf();
        faqVectors.clear();
        for (FaqItem item : faqItems) {
            faqVectors.add(computeTfIdfVector(item.question));
        }
        saveToRedis();
        log.info("FAQ vectors rebuilt: {} items", faqItems.size());
    }

    private void loadFaqData() {
        try (InputStream is = new ClassPathResource("faq-data.json").getInputStream()) {
            List<FaqItem> loaded = objectMapper.readValue(is, new TypeReference<List<FaqItem>>() {});
            faqItems.addAll(loaded);
        } catch (Exception e) {
            log.error("Failed to load faq-data.json, falling back to empty FAQ list", e);
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

    public List<FaqItem> search(String query, int topK) {
        Map<String, Double> queryVector = computeTfIdfVector(query);
        List<Map.Entry<FaqItem, Double>> scored = new ArrayList<>();
        for (int i = 0; i < faqItems.size(); i++) {
            double score = cosineSimilarity(queryVector, faqVectors.get(i));
            scored.add(new AbstractMap.SimpleEntry<>(faqItems.get(i), score));
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<FaqItem> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            if (scored.get(i).getValue() >= SIMILARITY_THRESHOLD) {
                results.add(scored.get(i).getKey());
            }
        }
        return results;
    }

    public String buildContext(String query) {
        List<FaqItem> matches = search(query, TOP_K);
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