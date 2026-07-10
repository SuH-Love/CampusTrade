package com.campustrade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GoodsVO {

    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private Long categoryId;
    private String categoryName;
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
    private Boolean isFavorited;
    private LocalDateTime createTime;
}