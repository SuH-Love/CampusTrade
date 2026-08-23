package com.campustrade.mq;

import com.campustrade.constant.MQConstant;
import com.campustrade.entity.Goods;
import com.campustrade.entity.GoodsCategory;
import com.campustrade.enum_.GoodsStatus;
import com.campustrade.mapper.GoodsCategoryMapper;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.service.ai.AiReviewService;
import com.campustrade.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GoodsAuditConsumer {

    private static final int MAX_RETRIES = 3;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;

    @Autowired
    private AiReviewService aiReviewService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = MQConstant.GOODS_AUDIT_QUEUE)
    public void handleGoodsAudit(Long goodsId, Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String idempotentKey = "mq:consumed:goods:audit:" + goodsId;
            Boolean consumed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
            if (consumed == null || !consumed) {
                log.info("商品审核消息已消费，跳过: goodsId={}", goodsId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Goods goods = goodsMapper.selectById(goodsId);
            if (goods == null) {
                log.warn("商品不存在: goodsId={}", goodsId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (!GoodsStatus.PENDING.getCode().equals(goods.getStatus())) {
                log.info("商品状态非PENDING，跳过AI审核: goodsId={}, status={}", goodsId, goods.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            String categoryName = "未知分类";
            if (goods.getCategoryId() != null) {
                List<GoodsCategory> categories = goodsCategoryMapper.selectByIds(java.util.Collections.singletonList(goods.getCategoryId()));
                if (categories != null && !categories.isEmpty()) {
                    categoryName = categories.get(0).getCategoryName();
                }
            }

            log.info("开始AI审核商品: goodsId={}, title={}, category={}", goodsId, goods.getTitle(), categoryName);

            AiReviewService.ReviewResult result = aiReviewService.review(
                    goods.getTitle(),
                    goods.getDescription(),
                    goods.getPrice() != null ? goods.getPrice().toPlainString() : "0",
                    categoryName
            );

            if (!result.isAiReviewed()) {
                log.info("AI审核不可用，保持PENDING状态等待人工审核: goodsId={}", goodsId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (result.isApproved()) {
                goods.setStatus(GoodsStatus.APPROVED.getCode());
                goodsMapper.updateById(goods);
                goodsMapper.clearRejectReason(goodsId);
                log.info("AI审核通过: goodsId={}, suggestedTitle={}", goodsId, result.getSuggestedTitle());
                notificationService.sendNotification(goods.getUserId(), "商品审核通过",
                        "您的商品「" + goods.getTitle() + "」已通过AI审核，可上架出售", "GOODS", goodsId);

                if (result.getSuggestedTitle() != null && !result.getSuggestedTitle().isEmpty()) {
                    String suggestionKey = "ai:suggestion:title:" + goodsId;
                    redisTemplate.opsForValue().set(suggestionKey, result.getSuggestedTitle(), 7, TimeUnit.DAYS);
                }
            } else {
                goods.setStatus(GoodsStatus.REJECTED.getCode());
                goods.setRejectReason(result.getReason());
                goodsMapper.updateById(goods);
                log.info("AI审核拒绝: goodsId={}, reason={}", goodsId, result.getReason());
                notificationService.sendNotification(goods.getUserId(), "商品审核未通过",
                        "您的商品「" + goods.getTitle() + "」未通过AI审核，原因：" + (result.getReason() != null ? result.getReason() : "无"), "GOODS", goodsId);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("商品AI审核消息处理失败: goodsId={}", goodsId, e);
            String retryKey = "mq:retry:goods:audit:" + goodsId;
            try {
                Long retryCount = redisTemplate.opsForValue().increment(retryKey);
                if (retryCount == null) retryCount = 1L;
                redisTemplate.expire(retryKey, 24, TimeUnit.HOURS);

                if (retryCount <= MAX_RETRIES) {
                    redisTemplate.delete("mq:consumed:goods:audit:" + goodsId);
                    channel.basicNack(deliveryTag, false, true);
                    log.warn("商品审核消息重入队列: goodsId={}, retry={}/{}", goodsId, retryCount, MAX_RETRIES);
                } else {
                    log.error("商品审核消息达到最大重试次数，发送到死信队列: goodsId={}", goodsId);
                    channel.basicNack(deliveryTag, false, false);
                }
            } catch (Exception ex) {
                log.error("NACK失败", ex);
            }
        }
    }
}