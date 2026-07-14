package com.campustrade.listener;

import com.campustrade.constant.RedisConstant;
import com.campustrade.entity.Goods;
import com.campustrade.entity.Order;
import com.campustrade.entity.OrderItem;
import com.campustrade.enum_.GoodsStatus;
import com.campustrade.enum_.OrderStatus;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.OrderItemMapper;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutListener extends KeyExpirationEventMessageListener {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = RedisConstant.ORDER_TIMEOUT_PREFIX;

    public OrderTimeoutListener(RedisMessageListenerContainer container) {
        super(container);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (!expiredKey.startsWith(PREFIX)) {
            return;
        }

        String orderIdStr = expiredKey.substring(PREFIX.length());
        Long orderId;
        try {
            orderId = Long.parseLong(orderIdStr);
        } catch (NumberFormatException e) {
            return;
        }

        String lockKey = PREFIX + "lock:" + orderId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, java.util.concurrent.TimeUnit.SECONDS);
        if (locked == null || !locked) {
            log.warn("Order timeout lock failed for order {}, skip", orderId);
            return;
        }

        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return;
            }
            if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
                return;
            }

            order.setStatus(OrderStatus.CANCELLED.getCode());
            order.setCancelReason("超时未支付，系统自动取消");
            order.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(order);

            List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
            for (OrderItem item : items) {
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods != null) {
                    int restoreQty = item.getQuantity() != null ? item.getQuantity() : 1;
                    goods.setStock(goods.getStock() != null ? goods.getStock() + restoreQty : restoreQty);
                    if (GoodsStatus.SOLD.getCode().equals(goods.getStatus())) {
                        goods.setStatus(GoodsStatus.ONLINE.getCode());
                    }
                    goodsMapper.updateById(goods);
                    redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + item.getGoodsId());
                    redisTemplate.delete(RedisConstant.GOODS_HOT_KEY);
                    redisTemplate.delete(RedisConstant.GOODS_RECOMMEND_KEY);
                }
            }

            notificationService.sendNotification(order.getBuyerId(), "订单超时取消",
                    "您的订单「" + order.getOrderNo() + "」因超时未支付已自动取消", "ORDER", order.getId());

            log.info("Order {} auto-cancelled by Redis key expiration", order.getOrderNo());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}