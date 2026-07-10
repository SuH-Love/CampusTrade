package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

    private Long orderId;
    private Long goodsId;
    private String goodsTitle;
    private String goodsImage;
    private BigDecimal price;
    private Integer quantity;
}