package com.campustrade.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartVO {
    private Long id;
    private Long goodsId;
    private String title;
    private String coverImage;
    private BigDecimal price;
    private Integer quantity;
    private String status;
    private Long sellerId;
}