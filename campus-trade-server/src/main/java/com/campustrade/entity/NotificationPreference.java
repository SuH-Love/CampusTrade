package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationPreference extends BaseEntity {
    private Long userId;
    private String notificationType;
    private Integer enabled;
}