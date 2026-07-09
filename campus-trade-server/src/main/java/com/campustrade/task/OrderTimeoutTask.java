package com.campustrade.task;

import com.campustrade.constant.RedisConstant;
import com.campustrade.entity.Order;
import com.campustrade.enum_.GoodsStatus;
import com.campustrade.enum_.OrderStatus;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.OrderItemMapper;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.entity.OrderItem;
import com.campustrade.entity.Goods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutTask {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrders() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);
        List<Order> orders = orderMapper.selectTimeoutPendingPay(timeout);
        for (Order order : orders) {
            order.setStatus(OrderStatus.CANCELLED.getCode());
            order.setCancelReason("超时未支付，系统自动取消");
            order.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(order);
            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            for (OrderItem item : items) {
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods != null && GoodsStatus.SOLD.getCode().equals(goods.getStatus())) {
                    goods.setStatus(GoodsStatus.ONLINE.getCode());
                    goodsMapper.updateById(goods);
                    redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + item.getGoodsId());
                    redisTemplate.delete(RedisConstant.GOODS_HOT_KEY);
                    redisTemplate.delete(RedisConstant.GOODS_RECOMMEND_KEY);
                }
            }
            log.info("Order {} auto-cancelled due to timeout", order.getOrderNo());
        }
    }
}