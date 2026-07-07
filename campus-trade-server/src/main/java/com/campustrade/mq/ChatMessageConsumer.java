package com.campustrade.mq;

import com.campustrade.entity.ChatMessage;
import com.campustrade.mapper.ChatMessageMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatMessageConsumer {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = "chat.save.queue")
    public void handleChatSave(ChatMessage message, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String idempotentKey = "mq:consumed:chat:save:" + message.getSenderId() + ":" + message.getCreateTime();
            Boolean consumed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
            if (consumed == null || !consumed) {
                log.info("聊天消息已消费，跳过: senderId={}", message.getSenderId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            chatMessageMapper.insert(message);
            channel.basicAck(deliveryTag, false);
            log.info("聊天消息持久化成功: senderId={}, receiverId={}", message.getSenderId(), message.getReceiverId());
        } catch (Exception e) {
            log.error("聊天消息持久化失败", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("NACK失败", ex); }
        }
    }
}
