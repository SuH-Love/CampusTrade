package com.campustrade.mapper;

import com.campustrade.entity.AiDocument;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiDocumentMapper {

    int insert(AiDocument document);

    int update(AiDocument document);

    int deleteById(Long id);

    AiDocument selectById(Long id);

    List<AiDocument> selectAll();

    List<AiDocument> selectByStatus(String status);

    int updateStatus(Long id, String status, String errorMessage, Integer chunkCount);
}