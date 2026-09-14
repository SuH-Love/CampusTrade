package com.campustrade.mapper;

import com.campustrade.entity.AiDocumentChunk;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiDocumentChunkMapper {

    int insert(AiDocumentChunk chunk);

    int insertBatch(List<AiDocumentChunk> chunks);

    int deleteByDocumentId(Long documentId);

    List<AiDocumentChunk> selectByDocumentId(Long documentId);

    List<AiDocumentChunk> selectAllReady();

    int countByDocumentId(Long documentId);
}