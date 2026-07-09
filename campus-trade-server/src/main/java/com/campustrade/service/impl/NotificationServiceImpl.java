package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.constant.MQConstant;
import com.campustrade.constant.RedisConstant;
import com.campustrade.entity.Notification;
import com.campustrade.entity.NotificationPreference;
import com.campustrade.mapper.NotificationMapper;
import com.campustrade.mapper.NotificationPreferenceMapper;
import com.campustrade.service.NotificationService;
import com.campustrade.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationPreferenceMapper notificationPreferenceMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                long val = ((Number) cached).longValue();
                return Result.success(val);
            }
        } catch (Exception e) {
            log.warn("Redis read unread count failed: {}", e.getMessage());
        }
        Long count = notificationMapper.selectUnreadCount(userId);
        try {
            redisTemplate.opsForValue().set(cacheKey, count, 300, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write unread count failed: {}", e.getMessage());
        }
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
        NotificationPreference pref = notificationPreferenceMapper.selectByUserAndType(userId, type);
        if (pref != null && pref.getEnabled() == 0) return;
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

        try {
            NotificationVO vo = toVO(notification);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), "/queue/notification", vo);
        } catch (Exception e) {
            log.warn("STOMP push notification failed for userId={}: {}", userId, e.getMessage());
        }
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