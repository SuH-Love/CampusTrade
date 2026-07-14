package com.campustrade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FundLog {

    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String type;
    private String status;
    private String tradeNo;
    private String remark;
    private LocalDateTime createTime;
}