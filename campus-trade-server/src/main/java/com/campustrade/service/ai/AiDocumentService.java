package com.campustrade.service.ai;

import com.campustrade.entity.AiDocument;
import com.campustrade.entity.AiDocumentChunk;
import com.campustrade.mapper.AiDocumentChunkMapper;
import com.campustrade.mapper.AiDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

@Slf4j
@Service
public class AiDocumentService {

    @Autowired private AiDocumentMapper documentMapper;
    @Autowired private AiDocumentChunkMapper chunkMapper;
    @Autowired private DocumentParser documentParser;
    @Autowired private TextChunker textChunker;
    @Autowired private DeepSeekClient deepSeekClient;
    @Autowired private VectorStore vectorStore;

    @Value("${ai.document.storage-path:/app/ai-documents}")
    private String storagePath;

    @Value("${ai.document.max-chunks:200}")
    private int maxChunks;

    private static final String VECTOR_PREFIX = "doc:";

    public AiDocument uploadDocument(String fileName, byte[] content) {
        String fileType = determineFileType(fileName);
        String savedPath = saveFile(fileName, content);

        AiDocument doc = new AiDocument();
        doc.setTitle(fileName);
        doc.setFileUrl(savedPath);
        doc.setFileType(fileType);
        doc.setFileSize((long) content.length);
        doc.setStatus("processing");
        doc.setChunkCount(0);
        documentMapper.insert(doc);

        processDocumentAsync(doc.getId(), content, fileName);
        return doc;
    }

    public void processDocumentAsync(Long docId, byte[] content, String fileName) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                processDocument(docId, content, fileName);
            } catch (Exception e) {
                log.error("Failed to process document {}: {}", docId, e.getMessage());
                documentMapper.updateStatus(docId, "error", e.getMessage(), 0);
            }
        });
    }

    private void processDocument(Long docId, byte[] content, String fileName) {
        DocumentParser.ParsedDocument parsed = documentParser.parse(content, fileName);

        if ("image".equals(parsed.fileType)) {
            String description = describeImage(content, fileName);
            parsed.pages.clear();
            parsed.pages.add(new DocumentParser.ParsedPage(description, 1));
        }

        List<TextChunker.Chunk> chunks = textChunker.chunkAll(parsed.pages);
        if (chunks.isEmpty()) {
            documentMapper.updateStatus(docId, "error", "文档内容为空", 0);
            return;
        }
        if (chunks.size() > maxChunks) {
            chunks = chunks.subList(0, maxChunks);
            log.warn("Document {} truncated to {} chunks", docId, maxChunks);
        }

        List<AiDocumentChunk> entityChunks = new ArrayList<>();
        List<String> textsForEmbedding = new ArrayList<>();
        for (TextChunker.Chunk chunk : chunks) {
            AiDocumentChunk entity = new AiDocumentChunk();
            entity.setDocumentId(docId);
            entity.setChunkIndex(chunk.chunkIndex);
            entity.setContent(chunk.content);
            entity.setPageNum(chunk.pageNum);
            entityChunks.add(entity);
            textsForEmbedding.add(chunk.content);
        }
        chunkMapper.insertBatch(entityChunks);

        if (deepSeekClient.isEmbeddingAvailable()) {
            List<float[]> embeddings = batchEmbeddings(textsForEmbedding);
            for (int i = 0; i < embeddings.size() && i < entityChunks.size(); i++) {
                String vectorId = VECTOR_PREFIX + docId + ":" + entityChunks.get(i).getId();
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", docId);
                metadata.put("chunkIndex", chunks.get(i).chunkIndex);
                metadata.put("pageNum", chunks.get(i).pageNum);
                metadata.put("title", fileName);
                metadata.put("content", chunks.get(i).content);
                vectorStore.upsert(vectorId, embeddings.get(i), metadata);
            }
            log.info("Document {} indexed: {} chunks with embeddings", docId, embeddings.size());
        }

        documentMapper.updateStatus(docId, "ready", null, chunks.size());
        log.info("Document {} processed: {} chunks", docId, chunks.size());
    }

    private String describeImage(byte[] content, String fileName) {
        if (!deepSeekClient.isEnabled()) return "[图片文件，AI服务不可用]";
        try {
            String base64 = Base64.getEncoder().encodeToString(content);
            String dataUrl = "data:image/" + getFileExtension(fileName) + ";base64," + base64;
            List<Map<String, Object>> msgs = new ArrayList<>();
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", dataUrl + "\n请详细描述这张图片的内容，包括文字、图表、场景等信息。");
            msgs.add(msg);
            String result = deepSeekClient.chat(msgs);
            return result != null ? result : "[图片描述生成失败]";
        } catch (Exception e) {
            log.warn("Failed to describe image: {}", e.getMessage());
            return "[图片描述失败: " + e.getMessage() + "]";
        }
    }

    private List<float[]> batchEmbeddings(List<String> texts) {
        int batchSize = 100;
        List<float[]> all = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<float[]> batch = deepSeekClient.embeddings(texts.subList(i, end));
            all.addAll(batch);
        }
        return all;
    }

    public List<VectorStore.SearchResult> searchRelevant(String query, int topK) {
        if (!deepSeekClient.isEmbeddingAvailable()) return Collections.emptyList();
        float[] queryEmb = deepSeekClient.embedding(query);
        if (queryEmb == null) return Collections.emptyList();
        return vectorStore.search(queryEmb, topK);
    }

    public String buildDocumentContext(String query) {
        List<VectorStore.SearchResult> results = searchRelevant(query, 3);
        if (results.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n\n## 文档知识（以下来自上传的文档资料）\n");
        Set<Long> seenDocs = new HashSet<>();
        for (VectorStore.SearchResult result : results) {
            if (result.score < 0.3) continue;
            Map<String, Object> meta = result.metadata;
            if (meta == null) continue;
            Long docId = ((Number) meta.get("documentId")).longValue();
            String title = (String) meta.getOrDefault("title", "未知文档");
            Integer pageNum = meta.get("pageNum") instanceof Number ? ((Number) meta.get("pageNum")).intValue() : null;
            String content = (String) meta.getOrDefault("content", "");

            sb.append("### 来源：").append(title);
            if (pageNum != null) sb.append("（第").append(pageNum).append("页）");
            sb.append("\n").append(content).append("\n\n");
        }
        return sb.toString();
    }

    public List<AiDocument> listDocuments() {
        return documentMapper.selectAll();
    }

    public AiDocument getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    public List<AiDocumentChunk> getChunks(Long docId) {
        return chunkMapper.selectByDocumentId(docId);
    }

    public void deleteDocument(Long id) {
        vectorStore.deleteByPrefix(VECTOR_PREFIX + id + ":");
        chunkMapper.deleteByDocumentId(id);
        documentMapper.deleteById(id);
        AiDocument doc = documentMapper.selectById(id);
        if (doc != null) {
            try { new File(doc.getFileUrl()).delete(); } catch (Exception ignored) {}
        }
    }

    private String saveFile(String fileName, byte[] content) {
        try {
            File dir = new File(storagePath);
            if (!dir.exists()) dir.mkdirs();
            String savedName = System.currentTimeMillis() + "_" + fileName;
            File file = new File(dir, savedName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to save file: {}", e.getMessage());
            return null;
        }
    }

    private String determineFileType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "word";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "text";
        if (lower.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) return "image";
        return "text";
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "jpg";
    }
}