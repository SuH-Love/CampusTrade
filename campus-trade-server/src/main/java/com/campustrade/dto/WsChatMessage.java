package com.campustrade.dto;

import lombok.Data;

@Data
public class WsChatMessage {

    private String type;
    private Long receiverId;
    private String content;
    private Integer messageType;
}