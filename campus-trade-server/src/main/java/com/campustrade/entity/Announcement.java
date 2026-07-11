package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Announcement extends BaseEntity {
    private String title;
    private String content;
    private Integer status;
    private Integer sortOrder;
}