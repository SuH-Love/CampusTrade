package com.campustrade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String notificationType;
    private Long relatedId;
    private Integer isRead;
    private LocalDateTime createTime;
}