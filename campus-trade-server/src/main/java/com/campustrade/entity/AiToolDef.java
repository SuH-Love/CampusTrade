package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiToolDef extends BaseEntity {

    private String toolName;
    private String displayName;
    private String toolGroup;
    private String description;
    private String parameters;
    private String handlerClass;
    private String handlerMethod;
    private String requiredRole;
    private Integer isWriteOperation;
    private Integer needConfirm;
    private Integer isActive;
    private Integer sortOrder;
    private Integer configVersion;
    private Long updatedBy;
}