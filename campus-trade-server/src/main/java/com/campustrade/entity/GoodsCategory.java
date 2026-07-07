package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsCategory extends BaseEntity {

    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private String icon;
    private Integer status;
}