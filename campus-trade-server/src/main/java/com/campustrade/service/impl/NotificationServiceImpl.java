package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.constant.MQConstant;
import com.campustrade.constant.RedisConstant;
import com.campustrade.entity.Notification;
import com.campustrade.mapper.NotificationMapper;
import com.campustrade.service.NotificationService;
import com.campustrade.vo.NotificationVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result<PageResult<NotificationVO>> listNotifications(Long userId, Integer isRead, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notification> list = notificationMapper.selectByUserId(userId, isRead, offset, pageSize);
        Long total = notificationMapper.selectCountByUserId(userId, isRead);
        List<NotificationVO> vos = list.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<Long> getUnreadCount(Long userId) {
        String cacheKey = RedisConstant.NOTIFY_USER_PREFIX + userId + ":unread";
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return Result.success((Long) cached);
        Long count = notificationMapper.selectUnreadCount(userId);
        redisTemplate.opsForValue().set(cacheKey, count, 300, TimeUnit.SECONDS);
        return Result.success(count);
    }

    @Override
    public Result<Void> markAsRead(Long userId, Long notificationId) {
        notificationMapper.markAsRead(notificationId, userId);
        redisTemplate.delete(RedisConstant.NOTIFY_USER_PREFIX + userId + ":unread");
        return Result.success();
    }

    @Override
    public Result<Void> markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
        redisTemplate.delete(RedisConstant.NOTIFY_USER_PREFIX + userId + ":unread");
        return Result.success();
    }

    @Override
    public Result<Void> deleteNotification(Long userId, Long notificationId) {
        notificationMapper.logicDeleteById(notificationId, userId);
        return Result.success();
    }

    @Override
    public void sendNotification(Long userId, String title, String content, String type, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotificationType(type);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notificationMapper.insert(notification);

        rabbitTemplate.convertAndSend(MQConstant.NOTIFY_EXCHANGE, MQConstant.NOTIFY_SEND_KEY, notification);
        redisTemplate.delete(RedisConstant.NOTIFY_USER_PREFIX + userId + ":unread");
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setUserId(n.getUserId());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setNotificationType(n.getNotificationType());
        vo.setRelatedId(n.getRelatedId());
        vo.setIsRead(n.getIsRead());
        vo.setCreateTime(n.getCreateTime());
        return vo;
    }
}