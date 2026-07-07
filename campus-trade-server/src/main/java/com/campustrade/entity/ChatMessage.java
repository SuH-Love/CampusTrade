package com.campustrade.entity;

import com.campustrade.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends BaseEntity {

    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer messageType;
    private Integer isRead;
}