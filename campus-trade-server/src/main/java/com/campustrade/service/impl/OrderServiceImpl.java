package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.constant.MQConstant;
import com.campustrade.constant.RedisConstant;
import com.campustrade.dto.OrderCreateDTO;
import com.campustrade.entity.*;
import com.campustrade.enum_.GoodsStatus;
import com.campustrade.enum_.OrderStatus;
import com.campustrade.mapper.*;
import com.campustrade.service.OrderService;
import com.campustrade.service.NotificationService;
import com.campustrade.vo.OrderItemVO;
import com.campustrade.vo.OrderVO;
import com.campustrade.util.SnowflakeIdUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> createOrder(Long buyerId, OrderCreateDTO dto) {
        String repeatKey = RedisConstant.REPEAT_PREFIX + buyerId + ":" + dto.getGoodsId();
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(repeatKey, "1", RedisConstant.REPEAT_TTL, TimeUnit.SECONDS);
        if (isFirst == null || !isFirst) {
            return Result.error(ResultCode.ORDER_REPEAT_SUBMIT);
        }

        Goods goods = goodsMapper.selectById(dto.getGoodsId());
        if (goods == null || !GoodsStatus.ONLINE.getCode().equals(goods.getStatus())) {
            return Result.error(ResultCode.GOODS_NOT_FOUND);
        }
        if (goods.getUserId().equals(buyerId)) {
            return Result.error(ResultCode.ORDER_STATUS_ERROR);
        }

        String orderNo = generateOrderNo();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getUserId());
        order.setTotalAmount(goods.getPrice());
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setGoodsId(goods.getId());
        item.setGoodsTitle(goods.getTitle());
        item.setGoodsImage(goods.getCoverImage());
        item.setPrice(goods.getPrice());
        orderItemMapper.insertBatch(List.of(item));

        goods.setStatus(GoodsStatus.SOLD.getCode());
        int goodsRows = goodsMapper.updateById(goods);
        if (goodsRows == 0) throw new RuntimeException("商品状态已变更，请刷新后重试");

        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goods.getId());
        redisTemplate.delete(RedisConstant.GOODS_HOT_KEY);
        redisTemplate.delete(RedisConstant.GOODS_RECOMMEND_KEY);

        rabbitTemplate.convertAndSend(MQConstant.ORDER_EXCHANGE, MQConstant.ORDER_CREATE_KEY, order.getId());

        notificationService.sendNotification(goods.getUserId(), "新订单通知",
                "您的商品「" + goods.getTitle() + "」有买家下单，请及时处理", "ORDER", order.getId());

        return Result.success(toVO(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId))
            return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus()) && !OrderStatus.PAID.getCode().equals(order.getStatus()))
            return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
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
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> payOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(order.getSellerId(), "订单支付通知",
                "买家已支付订单「" + order.getOrderNo() + "」，请尽快发货", "ORDER", order.getId());

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> shipOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getSellerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PAID.getCode().equals(order.getStatus())) return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.SHIPPING.getCode());
        order.setShipTime(LocalDateTime.now());

        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(order.getBuyerId(), "订单发货通知",
                "卖家已发货，订单「" + order.getOrderNo() + "」，请注意查收", "ORDER", order.getId());

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> finishOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.SHIPPING.getCode().equals(order.getStatus())) return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.FINISHED.getCode());
        order.setFinishTime(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(order.getSellerId(), "订单完成通知",
                "买家已确认收货，订单「" + order.getOrderNo() + "」已完成", "ORDER", order.getId());

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> refundOrder(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PAID.getCode().equals(order.getStatus()) && !OrderStatus.SHIPPING.getCode().equals(order.getStatus()))
            return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.REFUND.getCode());
        order.setCancelReason(reason);
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(order.getSellerId(), "退款申请",
                "买家申请订单「" + order.getOrderNo() + "」退款，原因：" + (reason != null ? reason : "无"), "ORDER", order.getId());

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> approveRefund(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getSellerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.REFUND.getCode().equals(order.getStatus()))
            return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
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

        notificationService.sendNotification(order.getBuyerId(), "退款成功",
                "卖家已同意订单「" + order.getOrderNo() + "」的退款申请", "ORDER", order.getId());

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectRefund(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getSellerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.REFUND.getCode().equals(order.getStatus()))
            return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setCancelReason(reason);
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(order.getBuyerId(), "退款被拒绝",
                "卖家拒绝了订单「" + order.getOrderNo() + "」的退款申请，原因：" + (reason != null ? reason : "无"), "ORDER", order.getId());

        return Result.success();
    }

    @Override
    public Result<OrderVO> getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId))
            return Result.error(ResultCode.ORDER_NOT_OWNER);
        return Result.success(toVO(order));
    }

    @Override
    public Result<PageResult<OrderVO>> listBuyerOrders(Long buyerId, String status, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Order> list = orderMapper.selectByBuyerId(buyerId, status, offset, pageSize);
        Long total = orderMapper.selectCountByBuyerId(buyerId, status);
        List<OrderVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<PageResult<OrderVO>> listSellerOrders(Long sellerId, String status, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Order> list = orderMapper.selectBySellerId(sellerId, status, offset, pageSize);
        Long total = orderMapper.selectCountBySellerId(sellerId, status);
        List<OrderVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<PageResult<OrderVO>> listOrdersByAdmin(String status, Integer pageNum, Integer pageSize) {
        return listAllOrders(status, pageNum, pageSize);
    }

    @Override
    public Result<PageResult<OrderVO>> listAllOrders(String status, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Order> list = orderMapper.selectAll(status, offset, pageSize);
        Long total = orderMapper.selectCountAll(status);
        List<OrderVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public long countOrders() {
        Long count = orderMapper.selectCountAll(null);
        return count != null ? count : 0L;
    }

    private String generateOrderNo() {
        return "CT" + SnowflakeIdUtil.getInstance().nextIdStr();
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        User buyer = userMapper.selectById(order.getBuyerId());
        if (buyer != null) vo.setBuyerName(buyer.getNickname() != null ? buyer.getNickname() : buyer.getUsername());
        User seller = userMapper.selectById(order.getSellerId());
        if (seller != null) vo.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());

        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    private List<OrderVO> toVOList(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return List.of();

        Set<Long> userIds = new HashSet<>();
        for (Order o : orders) {
            userIds.add(o.getBuyerId());
            userIds.add(o.getSellerId());
        }
        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setBuyerId(order.getBuyerId());
            vo.setSellerId(order.getSellerId());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setStatus(order.getStatus());
            vo.setPayTime(order.getPayTime());
            vo.setShipTime(order.getShipTime());
            vo.setFinishTime(order.getFinishTime());
            vo.setCancelTime(order.getCancelTime());
            vo.setCancelReason(order.getCancelReason());
            vo.setRemark(order.getRemark());
            vo.setCreateTime(order.getCreateTime());

            User buyer = userMap.get(order.getBuyerId());
            if (buyer != null) vo.setBuyerName(buyer.getNickname() != null ? buyer.getNickname() : buyer.getUsername());
            User seller = userMap.get(order.getSellerId());
            if (seller != null) vo.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());

            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setOrderId(item.getOrderId());
        vo.setGoodsId(item.getGoodsId());
        vo.setGoodsTitle(item.getGoodsTitle());
        vo.setGoodsImage(item.getGoodsImage());
        vo.setPrice(item.getPrice());
        return vo;
    }
}