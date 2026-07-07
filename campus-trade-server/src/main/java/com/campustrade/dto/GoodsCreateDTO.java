package com.campustrade.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class GoodsCreateDTO {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "商品标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价不能小于0.01")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "原价不能小于0.01")
    private BigDecimal originalPrice;

    private String coverImage;

    private String images;
}