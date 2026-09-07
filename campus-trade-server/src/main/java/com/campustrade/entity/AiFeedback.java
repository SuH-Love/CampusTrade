package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiFeedback extends BaseEntity {

    private Long userId;
    private String sessionId;
    private String messageId;
    private String userMessage;
    private String aiResponse;
    private Integer rating;
    private String feedback;
}