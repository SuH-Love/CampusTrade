package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TextChunker {

    @Value("${ai.document.chunk-size:500}")
    private int chunkSize;

    @Value("${ai.document.chunk-overlap:100}")
    private int chunkOverlap;

    public static class Chunk {
        public String content;
        public int chunkIndex;
        public int pageNum;
        public String metadata;

        public Chunk(String content, int chunkIndex, int pageNum, String metadata) {
            this.content = content;
            this.chunkIndex = chunkIndex;
            this.pageNum = pageNum;
            this.metadata = metadata;
        }
    }

    public List<Chunk> chunk(String text, int pageNum, String metadata) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return chunks;

        String cleaned = text.trim();
        List<String> segments = splitByParagraphs(cleaned);

        StringBuilder current = new StringBuilder();
        int chunkIndex = 0;
        String overlapText = "";

        for (String segment : segments) {
            if (current.length() + segment.length() + 1 > chunkSize && current.length() > 0) {
                String chunkContent = overlapText + current.toString().trim();
                if (!chunkContent.isEmpty()) {
                    chunks.add(new Chunk(chunkContent, chunkIndex++, pageNum, metadata));
                }
                overlapText = getTail(current.toString(), chunkOverlap);
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append("\n");
            current.append(segment);
        }

        if (current.length() > 0) {
            String chunkContent = overlapText + current.toString().trim();
            if (!chunkContent.isEmpty()) {
                chunks.add(new Chunk(chunkContent, chunkIndex++, pageNum, metadata));
            }
        }

        return chunks;
    }

    public List<Chunk> chunkAll(List<DocumentParser.ParsedPage> pages) {
        List<Chunk> allChunks = new ArrayList<>();
        int globalIndex = 0;
        for (DocumentParser.ParsedPage page : pages) {
            List<Chunk> pageChunks = chunk(page.text, page.pageNum, null);
            for (Chunk c : pageChunks) {
                c.chunkIndex = globalIndex++;
                allChunks.add(c);
            }
        }
        log.info("Chunked {} pages into {} chunks (size={}, overlap={})",
                pages.size(), allChunks.size(), chunkSize, chunkOverlap);
        return allChunks;
    }

    private List<String> splitByParagraphs(String text) {
        List<String> segments = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.length() <= chunkSize) {
                segments.add(trimmed);
            } else {
                for (int i = 0; i < trimmed.length(); i += chunkSize - chunkOverlap) {
                    int end = Math.min(i + chunkSize, trimmed.length());
                    segments.add(trimmed.substring(i, end));
                    if (end == trimmed.length()) break;
                }
            }
        }
        return segments;
    }

    private String getTail(String text, int maxLen) {
        if (text.length() <= maxLen) return text + "\n";
        return text.substring(text.length() - maxLen) + "\n";
    }
}