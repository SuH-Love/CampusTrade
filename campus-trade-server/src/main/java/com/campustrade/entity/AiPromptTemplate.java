package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiPromptTemplate extends BaseEntity {

    private String templateKey;
    private String templateName;
    private String category;
    private String content;
    private String variables;
    private String description;
    private Integer isActive;
    private Integer configVersion;
    private Long updatedBy;
}