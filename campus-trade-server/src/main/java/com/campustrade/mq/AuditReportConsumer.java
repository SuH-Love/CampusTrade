package com.campustrade.mq;

import com.campustrade.entity.Report;
import com.campustrade.mapper.ReportMapper;
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
public class AuditReportConsumer {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = "audit.report.queue")
    public void handleAuditReport(Long reportId, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String idempotentKey = "mq:consumed:audit:report:" + reportId;
            Boolean consumed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
            if (consumed == null || !consumed) {
                log.info("举报审核消息已消费，跳过: reportId={}", reportId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Report report = reportMapper.selectById(reportId);
            if (report != null && "PENDING".equals(report.getStatus())) {
                report.setStatus("PROCESSING");
                reportMapper.updateById(report);
            }
            channel.basicAck(deliveryTag, false);
            log.info("举报审核消息处理成功: reportId={}", reportId);
        } catch (Exception e) {
            log.error("举报审核消息处理失败", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("NACK失败", ex); }
        }
    }
}
