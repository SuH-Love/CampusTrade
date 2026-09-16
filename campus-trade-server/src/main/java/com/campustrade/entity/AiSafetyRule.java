package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiSafetyRule extends BaseEntity {

    private String ruleType;
    private String rulePattern;
    private String ruleAction;
    private String replacement;
    private String description;
    private Integer isActive;
    private Integer sortOrder;
    private Long updatedBy;
}