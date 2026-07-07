package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.vo.NotificationVO;

public interface NotificationService {

    Result<PageResult<NotificationVO>> listNotifications(Long userId, Integer isRead, Integer pageNum, Integer pageSize);

    Result<Long> getUnreadCount(Long userId);

    Result<Void> markAsRead(Long userId, Long notificationId);

    Result<Void> markAllAsRead(Long userId);

    Result<Void> deleteNotification(Long userId, Long notificationId);

    void sendNotification(Long userId, String title, String content, String type, Long relatedId);
}