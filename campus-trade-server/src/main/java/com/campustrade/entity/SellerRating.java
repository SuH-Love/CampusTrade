package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SellerRating extends BaseEntity {
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    private Integer rating;
    private String comment;
}