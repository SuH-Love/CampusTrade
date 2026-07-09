package com.campustrade.vo;

import lombok.Data;

@Data
public class BannerVO {

    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkUrl;
    private String bgColor;
    private Integer sortOrder;
    private Integer status;
    private String createTime;
}