package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    private String notificationType;
    private Long relatedId;
    private Integer isRead;
}