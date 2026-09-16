package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiQuickQuestion extends BaseEntity {

    private String question;
    private String category;
    private Integer sortOrder;
    private Integer isActive;
}