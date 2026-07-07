package com.campustrade.vo;

import lombok.Data;

@Data
public class OperationLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String requestUrl;
    private String ip;
    private Long duration;
    private Integer status;
    private String errorMsg;
    private String traceId;
    private String createTime;
}