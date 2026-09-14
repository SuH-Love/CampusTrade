package com.campustrade.service.ai;

import java.util.List;
import java.util.Map;

public interface VectorStore {

    void upsert(String id, float[] vector, Map<String, Object> metadata);

    void delete(String id);

    void deleteByPrefix(String prefix);

    List<SearchResult> search(float[] queryVector, int topK);

    int size();

    void clear();

    class SearchResult {
        public String id;
        public double score;
        public Map<String, Object> metadata;

        public SearchResult(String id, double score, Map<String, Object> metadata) {
            this.id = id;
            this.score = score;
            this.metadata = metadata;
        }
    }
}