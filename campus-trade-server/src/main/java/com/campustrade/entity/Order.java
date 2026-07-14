package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {

    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String remark;
    private Integer deliveryMethod;
    private String address;
    private String trackingNo;
    private String tradeNo;
    private String preRefundStatus;
    private Long sellerPaymentConfigId;
}