package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Cart extends BaseEntity {
    private Long userId;
    private Long goodsId;
    private Integer quantity;
}