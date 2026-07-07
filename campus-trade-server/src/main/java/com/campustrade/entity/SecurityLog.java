package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SecurityLog extends BaseEntity {

    private Long userId;
    private String username;
    private String eventType;
    private String ip;
    private String detail;
    private String traceId;
}