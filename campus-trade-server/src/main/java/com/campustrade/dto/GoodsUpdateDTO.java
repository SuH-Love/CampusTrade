package com.campustrade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsUpdateDTO {

    private Long categoryId;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String coverImage;
    private String images;
}