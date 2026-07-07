package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLog extends BaseEntity {

    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private String ip;
    private Long duration;
    private Integer status;
    private String errorMsg;
    private String traceId;
}