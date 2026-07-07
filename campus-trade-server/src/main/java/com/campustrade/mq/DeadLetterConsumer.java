package com.campustrade.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = "campus.dlx.queue")
    public void handleDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("死信消息接收: exchange={}, routingKey={}, body={}",
                message.getMessageProperties().getReceivedExchange(),
                message.getMessageProperties().getReceivedRoutingKey(),
                new String(message.getBody()));
    }
}