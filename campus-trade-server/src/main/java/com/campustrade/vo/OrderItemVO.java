package com.campustrade.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {

    private Long id;
    private Long orderId;
    private Long goodsId;
    private String goodsTitle;
    private String goodsImage;
    private BigDecimal price;
}