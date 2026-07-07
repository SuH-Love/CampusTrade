package com.campustrade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportVO {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private Integer targetType;
    private Long targetId;
    private String reason;
    private String description;
    private String images;
    private String status;
    private Long handlerId;
    private String handlerName;
    private String handleResult;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}