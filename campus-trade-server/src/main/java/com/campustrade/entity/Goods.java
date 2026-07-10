package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Goods extends BaseEntity {

    private Long userId;
    private Long categoryId;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String condition;
    private String coverImage;
    private String images;
    private String status;
    private String rejectReason;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer stock;
}