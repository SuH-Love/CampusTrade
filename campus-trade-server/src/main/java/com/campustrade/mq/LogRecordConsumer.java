package com.campustrade.mq;

import com.campustrade.entity.OperationLog;
import com.campustrade.mapper.OperationLogMapper;
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
public class LogRecordConsumer {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = "log.record.queue")
    public void handleLogRecord(OperationLog operationLog, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String idempotentKey = "mq:consumed:log:record:" + operationLog.getTraceId();
            Boolean consumed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
            if (consumed == null || !consumed) {
                log.info("日志记录消息已消费，跳过: traceId={}", operationLog.getTraceId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            operationLogMapper.insert(operationLog);
            channel.basicAck(deliveryTag, false);
            log.info("日志记录消息处理成功: module={}", operationLog.getModule());
        } catch (Exception e) {
            log.error("日志记录消息处理失败", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("NACK失败", ex); }
        }
    }
}
