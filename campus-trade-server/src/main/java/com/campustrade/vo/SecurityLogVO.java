package com.campustrade.vo;

import lombok.Data;

@Data
public class SecurityLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String eventType;
    private String ip;
    private String detail;
    private String traceId;
    private String createTime;
}