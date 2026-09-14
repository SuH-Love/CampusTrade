package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.vector-store.type", havingValue = "memory", matchIfMissing = true)
public class MemoryVectorStore implements VectorStore {

    private static class VectorEntry {
        float[] vector;
        Map<String, Object> metadata;
        VectorEntry(float[] vector, Map<String, Object> metadata) {
            this.vector = vector;
            this.metadata = metadata;
        }
    }

    private final ConcurrentHashMap<String, VectorEntry> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(String id, float[] vector, Map<String, Object> metadata) {
        store.put(id, new VectorEntry(vector, metadata));
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public void deleteByPrefix(String prefix) {
        store.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (queryVector == null || store.isEmpty()) return Collections.emptyList();
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, VectorEntry> entry : store.entrySet()) {
            double score = cosineSim(queryVector, entry.getValue().vector);
            results.add(new SearchResult(entry.getKey(), score, entry.getValue().metadata));
        }
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.subList(0, Math.min(topK, results.size()));
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public void clear() {
        store.clear();
    }

    private static double cosineSim(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}