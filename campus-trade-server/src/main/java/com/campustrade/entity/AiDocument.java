package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiDocument extends BaseEntity {

    private String title;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private String errorMessage;
}