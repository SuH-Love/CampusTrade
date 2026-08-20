package com.campustrade.config;

import com.campustrade.constant.MQConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        template.setReturnsCallback(returned -> {
            log.warn("MQ消息被返回: replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getReplyCode(), returned.getReplyText(),
                    returned.getExchange(), returned.getRoutingKey());
        });
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("MQ消息未确认: cause={}", cause);
            }
        });
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        return factory;
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(MQConstant.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(MQConstant.CHAT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange auditExchange() {
        return new DirectExchange(MQConstant.AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(MQConstant.NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(MQConstant.LOG_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(MQConstant.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange goodsAuditExchange() {
        return new DirectExchange(MQConstant.GOODS_AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(MQConstant.ORDER_CREATE_QUEUE)
                .withArgument("x-dead-letter-exchange", MQConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MQConstant.DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    public Queue chatSaveQueue() {
        return QueueBuilder.durable(MQConstant.CHAT_SAVE_QUEUE)
                .withArgument("x-dead-letter-exchange", MQConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MQConstant.DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    public Queue auditReportQueue() {
        return QueueBuilder.durable(MQConstant.AUDIT_REPORT_QUEUE)
                .withArgument("x-dead-letter-exchange", MQConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MQConstant.DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    public Queue notifySendQueue() {
        return QueueBuilder.durable(MQConstant.NOTIFY_SEND_QUEUE)
                .withArgument("x-dead-letter-exchange", MQConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MQConstant.DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    public Queue logRecordQueue() {
        return QueueBuilder.durable(MQConstant.LOG_RECORD_QUEUE)
                .withArgument("x-dead-letter-exchange", MQConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MQConstant.DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(MQConstant.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Queue goodsAuditQueue() {
        return QueueBuilder.durable(MQConstant.GOODS_AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", MQConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MQConstant.DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(orderExchange()).with(MQConstant.ORDER_CREATE_KEY);
    }

    @Bean
    public Binding chatSaveBinding() {
        return BindingBuilder.bind(chatSaveQueue()).to(chatExchange()).with(MQConstant.CHAT_SAVE_KEY);
    }

    @Bean
    public Binding auditReportBinding() {
        return BindingBuilder.bind(auditReportQueue()).to(auditExchange()).with(MQConstant.AUDIT_REPORT_KEY);
    }

    @Bean
    public Binding notifySendBinding() {
        return BindingBuilder.bind(notifySendQueue()).to(notifyExchange()).with(MQConstant.NOTIFY_SEND_KEY);
    }

    @Bean
    public Binding logRecordBinding() {
        return BindingBuilder.bind(logRecordQueue()).to(logExchange()).with(MQConstant.LOG_RECORD_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(MQConstant.DEAD_LETTER_KEY);
    }

    @Bean
    public Binding goodsAuditBinding() {
        return BindingBuilder.bind(goodsAuditQueue()).to(goodsAuditExchange()).with(MQConstant.GOODS_AUDIT_KEY);
    }
}