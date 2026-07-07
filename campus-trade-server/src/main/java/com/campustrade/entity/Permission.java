package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Permission extends BaseEntity {

    private String permissionName;
    private String permissionCode;
    private Integer resourceType;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
}