package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Report extends BaseEntity {

    private Long reporterId;
    private Integer targetType;
    private Long targetId;
    private String reason;
    private String description;
    private String images;
    private String status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handleTime;
}