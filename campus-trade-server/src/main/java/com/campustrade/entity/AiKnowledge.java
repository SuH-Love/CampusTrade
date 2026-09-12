package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledge extends BaseEntity {

    private String title;
    private String keywords;
    private String content;
    private Integer enabled;
    private Integer sortOrder;
}