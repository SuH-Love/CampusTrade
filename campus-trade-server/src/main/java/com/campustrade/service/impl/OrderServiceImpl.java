package com.campustrade.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.config.AlipayConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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

    @Autowired
    private SellerRatingMapper sellerRatingMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private FundLogMapper fundLogMapper;

    @Autowired
    private PaymentConfigMapper paymentConfigMapper;

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

        int quantity = dto.getQuantity() != null && dto.getQuantity() > 0 ? dto.getQuantity() : 1;
        int stock = goods.getStock() != null ? goods.getStock() : 1;
        if (quantity > stock) {
            return Result.error(400, "库存不足，当前库存：" + stock);
        }

        String orderNo = generateOrderNo();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getUserId());
        order.setTotalAmount(goods.getPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setRemark(dto.getRemark());
        order.setDeliveryMethod("DELIVERY".equals(dto.getDeliveryMethod()) ? 1 : 0);
        order.setAddress(dto.getDeliveryAddress());

        try {
            PaymentConfig sellerPayment = paymentConfigMapper.selectDefaultByUserId(goods.getUserId());
            if (sellerPayment != null) {
                order.setSellerPaymentConfigId(sellerPayment.getId());
            }
        } catch (Exception e) {
            log.debug("Payment config not available for seller {}", goods.getUserId());
        }

        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setGoodsId(goods.getId());
        item.setGoodsTitle(goods.getTitle());
        item.setGoodsImage(goods.getCoverImage());
        item.setPrice(goods.getPrice());
        item.setQuantity(quantity);
        orderItemMapper.insertBatch(List.of(item));

        int decRows = goodsMapper.decrementStock(goods.getId(), quantity);
        if (decRows == 0) throw new RuntimeException("库存不足，请刷新后重试");

        Goods updatedGoods = goodsMapper.selectById(goods.getId());
        if (updatedGoods != null && updatedGoods.getStock() != null && updatedGoods.getStock() <= 0) {
            Goods statusUpdate = new Goods();
            statusUpdate.setId(goods.getId());
            statusUpdate.setStatus(GoodsStatus.SOLD.getCode());
            statusUpdate.setVersion(updatedGoods.getVersion());
            goodsMapper.updateById(statusUpdate);
        }

        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goods.getId());
        redisTemplate.delete(RedisConstant.GOODS_HOT_KEY);
        redisTemplate.delete(RedisConstant.GOODS_RECOMMEND_KEY);

        redisTemplate.opsForValue().set(RedisConstant.ORDER_TIMEOUT_PREFIX + order.getId(), String.valueOf(order.getId()),
                RedisConstant.ORDER_TIMEOUT_TTL, TimeUnit.SECONDS);

        rabbitTemplate.convertAndSend(MQConstant.ORDER_EXCHANGE, MQConstant.ORDER_CREATE_KEY, order.getId());

        notificationService.sendNotification(goods.getUserId(), "新订单通知",
                "您的商品「" + goods.getTitle() + "」有买家下单，请及时处理", "ORDER", order.getId());

        try {
            Cart cartItem = cartMapper.selectByUserAndGoods(buyerId, goods.getId());
            if (cartItem != null) {
                cartMapper.deleteById(cartItem.getId());
            }
        } catch (Exception ignored) {}

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

        redisTemplate.delete(RedisConstant.ORDER_TIMEOUT_PREFIX + orderId);

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
        return Result.success();
    }

    @Override
    public Result<Void> payOrder(Long userId, Long orderId) {
        if (!alipayConfig.isConfigured()) {
            return payOrderFallback(userId, orderId);
        }
        Result<String> payResult = createPayment(userId, orderId);
        if (payResult.getCode() != 200) {
            return Result.error(payResult.getCode(), payResult.getMessage());
        }
        return Result.success();
    }

    @Override
    public Result<String> createPayment(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) return Result.error(ResultCode.ORDER_STATUS_ERROR);

        if (!alipayConfig.isConfigured()) {
            return Result.error(503, "支付宝未配置，请联系管理员");
        }

        String lockKey = RedisConstant.ORDER_TIMEOUT_PREFIX + "lock:" + orderId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            return Result.error(409, "订单正在处理中，请稍后重试");
        }

        try {
            Order freshOrder = orderMapper.selectById(orderId);
            if (freshOrder == null || !OrderStatus.PENDING_PAY.getCode().equals(freshOrder.getStatus())) {
                return Result.error(ResultCode.ORDER_STATUS_ERROR);
            }

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayConfig.getEffectiveNotifyUrl());
            request.setReturnUrl(alipayConfig.getEffectiveReturnUrl() + orderId);

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", order.getOrderNo());
            bizContent.put("total_amount", order.getTotalAmount().toPlainString());
            bizContent.put("subject", "CampusTrade订单-" + order.getOrderNo());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toString());

            String form = alipayClient.pageExecute(request).getBody();
            log.info("Alipay payment created for order: {}", order.getOrderNo());
            return Result.success(form);
        } catch (AlipayApiException e) {
            log.error("Alipay create payment failed: {}", e.getMessage(), e);
            return Result.error(500, "创建支付失败: " + e.getErrMsg());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handlePayNotify(Map<String, String> params) {
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params, alipayConfig.getEffectiveAlipayPublicKey(), "UTF-8", alipayConfig.getSignType());
            if (!signVerified) {
                log.error("Alipay notify sign verification failed");
                return Result.error(400, "验签失败");
            }
        } catch (AlipayApiException e) {
            log.error("Alipay sign verify error: {}", e.getMessage());
            return Result.error(400, "验签异常");
        }

        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return Result.success();
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");

        Order order = orderMapper.selectByOrderNo(outTradeNo);
        if (order == null) {
            log.error("Alipay notify: order not found for out_trade_no={}", outTradeNo);
            return Result.error(ResultCode.ORDER_NOT_FOUND);
        }

        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.info("Alipay notify: order {} already paid, ignore", outTradeNo);
            return Result.success();
        }

        String lockKey = RedisConstant.ORDER_TIMEOUT_PREFIX + "lock:" + order.getId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            log.warn("Alipay notify: order {} lock failed, retry later", outTradeNo);
            return Result.error(409, "处理中");
        }

        try {
            Order freshOrder = orderMapper.selectById(order.getId());
            if (freshOrder == null || !OrderStatus.PENDING_PAY.getCode().equals(freshOrder.getStatus())) {
                return Result.success();
            }

            freshOrder.setStatus(OrderStatus.PAID.getCode());
            freshOrder.setPayTime(LocalDateTime.now());
            freshOrder.setTradeNo(tradeNo);
            orderMapper.updateById(freshOrder);

            redisTemplate.delete(RedisConstant.ORDER_TIMEOUT_PREFIX + order.getId());

            FundLog fundLog = new FundLog();
            fundLog.setOrderId(order.getId());
            fundLog.setUserId(order.getBuyerId());
            fundLog.setAmount(order.getTotalAmount());
            fundLog.setType("PAY");
            fundLog.setStatus("SUCCESS");
            fundLog.setTradeNo(tradeNo);
            fundLog.setRemark("买家支付");
            safeInsertFundLog(fundLog);

            FundLog freezeLog = new FundLog();
            freezeLog.setOrderId(order.getId());
            freezeLog.setUserId(order.getSellerId());
            freezeLog.setAmount(order.getTotalAmount());
            freezeLog.setType("FREEZE");
            freezeLog.setStatus("SUCCESS");
            freezeLog.setTradeNo(tradeNo);
            freezeLog.setRemark("担保冻结");
            safeInsertFundLog(freezeLog);

            notificationService.sendNotification(order.getSellerId(), "订单支付通知",
                    "买家已支付订单「" + order.getOrderNo() + "」，请尽快发货", "ORDER", order.getId());

            log.info("Alipay notify: order {} paid successfully, tradeNo={}", outTradeNo, tradeNo);
            return Result.success();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private Result<Void> payOrderFallback(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) return Result.error(ResultCode.ORDER_STATUS_ERROR);

        String lockKey = RedisConstant.ORDER_TIMEOUT_PREFIX + "lock:" + orderId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            return Result.error(409, "订单正在处理中");
        }

        try {
            order.setStatus(OrderStatus.PAID.getCode());
            order.setPayTime(LocalDateTime.now());
            int rows = orderMapper.updateById(order);
            if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

            redisTemplate.delete(RedisConstant.ORDER_TIMEOUT_PREFIX + orderId);

            FundLog fundLog = new FundLog();
            fundLog.setOrderId(order.getId());
            fundLog.setUserId(order.getBuyerId());
            fundLog.setAmount(order.getTotalAmount());
            fundLog.setType("PAY");
            fundLog.setStatus("SUCCESS");
            fundLog.setRemark("模拟支付");
            safeInsertFundLog(fundLog);

            notificationService.sendNotification(order.getSellerId(), "订单支付通知",
                    "买家已支付订单「" + order.getOrderNo() + "」，请尽快发货", "ORDER", order.getId());

            return Result.success();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> shipOrder(Long userId, Long orderId, String trackingNo) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getSellerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PAID.getCode().equals(order.getStatus())) return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.SHIPPING.getCode());
        order.setShipTime(LocalDateTime.now());
        if (trackingNo != null && !trackingNo.trim().isEmpty()) {
            order.setTrackingNo(trackingNo.trim());
        }

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
        order.setStatus(OrderStatus.PENDING_REVIEW.getCode());
        order.setFinishTime(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        FundLog settleLog = new FundLog();
        settleLog.setOrderId(order.getId());
        settleLog.setUserId(order.getSellerId());
        settleLog.setAmount(order.getTotalAmount());
        settleLog.setType("SETTLE");
        settleLog.setStatus("SUCCESS");
        settleLog.setTradeNo(order.getTradeNo());
        settleLog.setRemark("担保结算给卖家");
        safeInsertFundLog(settleLog);

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
        order.setPreRefundStatus(order.getStatus());
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

        if (alipayConfig.isConfigured() && order.getTradeNo() != null) {
            try {
                AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();
                JSONObject bizContent = new JSONObject();
                bizContent.put("out_trade_no", order.getOrderNo());
                bizContent.put("refund_amount", order.getTotalAmount().toPlainString());
                bizContent.put("refund_reason", "卖家同意退款");
                refundRequest.setBizContent(bizContent.toString());

                AlipayTradeRefundResponse refundResponse = alipayClient.execute(refundRequest);
                if (!refundResponse.isSuccess()) {
                    log.error("Alipay refund failed for order {}: {}", order.getOrderNo(), refundResponse.getSubMsg());
                    return Result.error(500, "支付宝退款失败: " + refundResponse.getSubMsg());
                }
                log.info("Alipay refund success for order: {}", order.getOrderNo());
            } catch (AlipayApiException e) {
                log.error("Alipay refund error: {}", e.getMessage(), e);
                return Result.error(500, "支付宝退款异常: " + e.getErrMsg());
            }
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        restoreGoodsStock(orderId);

        FundLog refundLog = new FundLog();
        refundLog.setOrderId(order.getId());
        refundLog.setUserId(order.getBuyerId());
        refundLog.setAmount(order.getTotalAmount());
        refundLog.setType("REFUND");
        refundLog.setStatus("SUCCESS");
        refundLog.setTradeNo(order.getTradeNo());
        refundLog.setRemark("退款成功");
        safeInsertFundLog(refundLog);

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

        String restoreStatus = order.getPreRefundStatus() != null ? order.getPreRefundStatus() : OrderStatus.PAID.getCode();
        order.setStatus(restoreStatus);
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
        List<Order> list = orderMapper.selectByBuyerId(buyerId, status, offset, pageSize, null, null);
        Long total = orderMapper.selectCountByBuyerId(buyerId, status, null, null);
        List<OrderVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<PageResult<OrderVO>> listSellerOrders(Long sellerId, String status, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Order> list = orderMapper.selectBySellerId(sellerId, status, offset, pageSize, null, null);
        Long total = orderMapper.selectCountBySellerId(sellerId, status, null, null);
        List<OrderVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<PageResult<OrderVO>> listOrdersByAdmin(String status, Integer pageNum, Integer pageSize) {
        return listAllOrders(null, status, null, null, pageNum, pageSize);
    }

    @Override
    public Result<PageResult<OrderVO>> listAllOrders(String orderNo, String status, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Order> list = orderMapper.selectAll(orderNo, status, startDate, endDate, offset, pageSize);
        Long total = orderMapper.selectCountAll(orderNo, status, startDate, endDate);
        List<OrderVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rateOrder(Long userId, Long orderId, Integer rating, String comment) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(userId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!"PENDING_REVIEW".equals(order.getStatus())) return Result.error(ResultCode.ORDER_STATUS_ERROR);
        order.setStatus(OrderStatus.FINISHED.getCode());
        order.setFinishTime(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        SellerRating sellerRating = new SellerRating();
        sellerRating.setOrderId(orderId);
        sellerRating.setBuyerId(userId);
        sellerRating.setSellerId(order.getSellerId());
        sellerRating.setRating(rating);
        sellerRating.setComment(comment);
        sellerRatingMapper.insert(sellerRating);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> modifyPrice(Long sellerId, Long orderId, java.math.BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return Result.error(400, "价格必须大于0");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        if (!order.getSellerId().equals(sellerId)) return Result.error(ResultCode.ORDER_NOT_OWNER);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus()))
            return Result.error(ResultCode.ORDER_STATUS_ERROR);

        java.math.BigDecimal oldPrice = order.getTotalAmount();
        order.setTotalAmount(newPrice);
        int rows = orderMapper.updateById(order);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        if (!items.isEmpty()) {
            orderItemMapper.updatePriceById(items.get(0).getId(), newPrice);
        }

        notificationService.sendNotification(order.getBuyerId(), "订单价格变更",
                "卖家已将订单「" + order.getOrderNo() + "」金额从 ¥" + oldPrice + " 修改为 ¥" + newPrice + "，请确认后支付",
                "ORDER", order.getId());

        return Result.success();
    }

    @Override
    public long countOrders() {
        Long count = orderMapper.selectCountAll(null, null, null, null);
        return count != null ? count : 0L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> adminApproveRefund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);

        if (alipayConfig.isConfigured() && order.getTradeNo() != null) {
            try {
                AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();
                JSONObject bizContent = new JSONObject();
                bizContent.put("out_trade_no", order.getOrderNo());
                bizContent.put("refund_amount", order.getTotalAmount().toPlainString());
                bizContent.put("refund_reason", "管理员同意退款");
                refundRequest.setBizContent(bizContent.toString());

                AlipayTradeRefundResponse refundResponse = alipayClient.execute(refundRequest);
                if (!refundResponse.isSuccess()) {
                    log.error("Admin alipay refund failed for order {}: {}", order.getOrderNo(), refundResponse.getSubMsg());
                }
            } catch (AlipayApiException e) {
                log.error("Admin alipay refund error: {}", e.getMessage(), e);
            }
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        restoreGoodsStock(orderId);

        FundLog refundLog = new FundLog();
        refundLog.setOrderId(order.getId());
        refundLog.setUserId(order.getBuyerId());
        refundLog.setAmount(order.getTotalAmount());
        refundLog.setType("REFUND");
        refundLog.setStatus("SUCCESS");
        refundLog.setTradeNo(order.getTradeNo());
        refundLog.setRemark("管理员退款");
        safeInsertFundLog(refundLog);

        notificationService.sendNotification(order.getBuyerId(), "退款成功",
                "管理员同意了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        notificationService.sendNotification(order.getSellerId(), "退款通知",
                "管理员同意了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> adminRejectRefund(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        String restoreStatus = order.getPreRefundStatus() != null ? order.getPreRefundStatus() : OrderStatus.PAID.getCode();
        order.setStatus(restoreStatus);
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        notificationService.sendNotification(order.getBuyerId(), "退款被拒绝",
                "管理员拒绝了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        notificationService.sendNotification(order.getSellerId(), "退款通知",
                "管理员拒绝了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        return Result.success();
    }

    private String generateOrderNo() {
        return "CT" + SnowflakeIdUtil.getInstance().nextIdStr();
    }

    private void restoreGoodsStock(Long orderId) {
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
        vo.setDeliveryMethod(order.getDeliveryMethod());
        vo.setAddress(order.getAddress());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setTradeNo(order.getTradeNo());
        vo.setPreRefundStatus(order.getPreRefundStatus());
        vo.setSellerPaymentConfigId(order.getSellerPaymentConfigId());
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
            vo.setDeliveryMethod(order.getDeliveryMethod());
            vo.setAddress(order.getAddress());
            vo.setTrackingNo(order.getTrackingNo());
            vo.setTradeNo(order.getTradeNo());
            vo.setPreRefundStatus(order.getPreRefundStatus());
            vo.setSellerPaymentConfigId(order.getSellerPaymentConfigId());
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
        vo.setQuantity(item.getQuantity());
        return vo;
    }

    private void safeInsertFundLog(FundLog fundLog) {
        try {
            fundLogMapper.insert(fundLog);
        } catch (Exception e) {
            log.warn("FundLog insert failed: {}", e.getMessage());
        }
    }
}