package com.campustrade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentConfigVO {

    private Long id;
    private Long userId;
    private String paymentType;
    private String alipayAccount;
    private String realName;
    private Integer isDefault;
    private String status;
    private LocalDateTime createTime;
}