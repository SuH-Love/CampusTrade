package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Banner extends BaseEntity {

    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkUrl;
    private String bgColor;
    private String buttonText;
    private Integer sortOrder;
    private Integer status;
}