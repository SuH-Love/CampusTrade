package com.campustrade.mq;

import com.campustrade.entity.OperationLog;
import com.campustrade.mapper.OperationLogMapper;
import com.campustrade.util.SnowflakeIdUtil;
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
public class OrderCreateConsumer {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = "order.create.queue")
    public void handleOrderCreate(Long orderId, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String idempotentKey = "mq:consumed:order:create:" + orderId;
            Boolean consumed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
            if (consumed == null || !consumed) {
                log.info("订单创建消息已消费，跳过: orderId={}", orderId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            OperationLog opLog = new OperationLog();
            opLog.setModule("order");
            opLog.setOperation("订单创建");
            opLog.setTraceId(String.valueOf(orderId));
            operationLogMapper.insert(opLog);
            channel.basicAck(deliveryTag, false);
            log.info("订单创建消息处理成功: orderId={}", orderId);
        } catch (Exception e) {
            log.error("订单创建消息处理失败", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("NACK失败", ex); }
        }
    }
}
