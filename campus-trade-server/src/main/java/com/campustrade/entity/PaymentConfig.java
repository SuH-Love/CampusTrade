package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentConfig extends BaseEntity {

    private Long userId;
    private String paymentType;
    private String alipayAccount;
    private String realName;
    private Integer isDefault;
    private String status;
}