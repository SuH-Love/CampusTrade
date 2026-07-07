package com.campustrade.mq;

import com.campustrade.entity.Notification;
import com.campustrade.constant.RedisConstant;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotifySendConsumer {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = "notify.send.queue")
    public void handleNotifySend(Notification notification, Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (notification.getUserId() != null) {
                String unreadKey = RedisConstant.NOTIFY_USER_PREFIX + notification.getUserId() + ":unread";
                redisTemplate.delete(unreadKey);
            }
            channel.basicAck(deliveryTag, false);
            log.info("通知发送消息处理成功: userId={}", notification.getUserId());
        } catch (Exception e) {
            log.error("通知发送消息处理失败", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("NACK失败", ex); }
        }
    }
}
