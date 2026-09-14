package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.AiDocument;
import com.campustrade.entity.AiDocumentChunk;
import com.campustrade.service.ai.AiDocumentService;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "AI文档知识库接口")
@RestController
@RequestMapping("/api/ai/document")
public class AiDocumentController {

    @Autowired
    private AiDocumentService documentService;

    @ApiOperation("上传文档到知识库")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (!SecurityUtil.isAdmin()) return Result.error(403, "无权限");
        if (file == null || file.isEmpty()) return Result.error(400, "文件不能为空");
        if (file.getSize() > 50 * 1024 * 1024) return Result.error(400, "文件大小不能超过50MB");

        try {
            String fileName = file.getOriginalFilename();
            byte[] content = file.getBytes();
            AiDocument doc = documentService.uploadDocument(fileName, content);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", doc.getId());
            result.put("title", doc.getTitle());
            result.put("fileType", doc.getFileType());
            result.put("status", doc.getStatus());
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to upload document", e);
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }

    @ApiOperation("获取文档列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        if (!SecurityUtil.isAdmin()) return Result.error(403, "无权限");
        List<AiDocument> docs = documentService.listDocuments();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiDocument doc : docs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", doc.getId());
            item.put("title", doc.getTitle());
            item.put("fileType", doc.getFileType());
            item.put("fileSize", doc.getFileSize());
            item.put("status", doc.getStatus());
            item.put("chunkCount", doc.getChunkCount());
            item.put("errorMessage", doc.getErrorMessage());
            item.put("createTime", doc.getCreateTime());
            result.add(item);
        }
        return Result.success(result);
    }

    @ApiOperation("获取文档详情（含分块）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        if (!SecurityUtil.isAdmin()) return Result.error(403, "无权限");
        AiDocument doc = documentService.getDocument(id);
        if (doc == null) return Result.error(404, "文档不存在");
        List<AiDocumentChunk> chunks = documentService.getChunks(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", doc.getId());
        result.put("title", doc.getTitle());
        result.put("fileType", doc.getFileType());
        result.put("fileSize", doc.getFileSize());
        result.put("status", doc.getStatus());
        result.put("chunkCount", doc.getChunkCount());
        result.put("errorMessage", doc.getErrorMessage());
        result.put("createTime", doc.getCreateTime());
        List<Map<String, Object>> chunkList = new ArrayList<>();
        for (AiDocumentChunk chunk : chunks) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", chunk.getId());
            c.put("chunkIndex", chunk.getChunkIndex());
            c.put("content", chunk.getContent());
            c.put("pageNum", chunk.getPageNum());
            chunkList.add(c);
        }
        result.put("chunks", chunkList);
        return Result.success(result);
    }

    @ApiOperation("删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!SecurityUtil.isAdmin()) return Result.error(403, "无权限");
        documentService.deleteDocument(id);
        return Result.success();
    }
}