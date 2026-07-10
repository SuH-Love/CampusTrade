package com.campustrade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class OrderCreateDTO {

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    private Integer quantity;
    private String remark;
    private String deliveryMethod;
    private String deliveryAddress;
}