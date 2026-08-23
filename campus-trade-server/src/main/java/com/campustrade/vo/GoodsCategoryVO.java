package com.campustrade.vo;

import lombok.Data;

@Data
public class GoodsCategoryVO {
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private String icon;
    private Integer status;
    private String createTime;
    private Integer goodsCount;
}