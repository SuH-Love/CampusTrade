package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiDocumentChunk extends BaseEntity {

    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer pageNum;
    private String imageUrls;
    private String metadata;
}