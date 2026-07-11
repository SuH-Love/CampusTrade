package com.campustrade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SellerRatingVO {
    private Long id;
    private Long orderId;
    private Long buyerId;
    private String buyerName;
    private String buyerAvatar;
    private Long sellerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createTime;
}