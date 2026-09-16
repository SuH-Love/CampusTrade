package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiConfigVersion extends BaseEntity {

    private String configType;
    private Long configId;
    private String configKey;
    private Integer configVersion;
    private String snapshot;
    private String changeNote;
    private Long createdBy;
}