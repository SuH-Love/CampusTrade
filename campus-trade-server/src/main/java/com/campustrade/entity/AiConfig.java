package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiConfig extends BaseEntity {

    private String configGroup;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Integer isActive;
    private Long updatedBy;
}