package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.config.AlipayConfig;
import com.campustrade.dto.OrderCreateDTO;
import com.campustrade.entity.FundLog;
import com.campustrade.mapper.FundLogMapper;
import com.campustrade.service.OrderService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Api(tags = "订单接口")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private FundLogMapper fundLogMapper;

    @ApiOperation("创建订单")
    @PostMapping
    public Result<OrderVO> createOrder(@Validated @RequestBody OrderCreateDTO dto) {
        return orderService.createOrder(SecurityUtil.requireCurrentUserId(), dto);
    }

    @ApiOperation("取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return orderService.cancelOrder(SecurityUtil.requireCurrentUserId(), id, reason);
    }

    @ApiOperation("支付（兼容旧接口）")
    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id) {
        return orderService.payOrder(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("创建支付宝支付")
    @PostMapping("/{id}/create-payment")
    public Result<String> createPayment(@PathVariable Long id) {
        return orderService.createPayment(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("支付宝异步通知")
    @PostMapping("/pay/notify")
    public String payNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }

        log.info("Alipay notify received: out_trade_no={}", params.get("out_trade_no"));

        try {
            Result<Void> result = orderService.handlePayNotify(params);
            if (result.getCode() == 200) {
                return "success";
            }
        } catch (Exception e) {
            log.error("Alipay notify handling error: {}", e.getMessage(), e);
        }
        return "failure";
    }

    @ApiOperation("发货")
    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(@PathVariable Long id, @RequestParam(required = false) String trackingNo) {
        return orderService.shipOrder(SecurityUtil.requireCurrentUserId(), id, trackingNo);
    }

    @ApiOperation("确认收货")
    @PutMapping("/{id}/finish")
    public Result<Void> finishOrder(@PathVariable Long id) {
        return orderService.finishOrder(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("退款")
    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return orderService.refundOrder(SecurityUtil.requireCurrentUserId(), id, reason);
    }

    @ApiOperation("同意退款")
    @PutMapping("/{id}/approve-refund")
    public Result<Void> approveRefund(@PathVariable Long id) {
        return orderService.approveRefund(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("拒绝退款")
    @PutMapping("/{id}/reject-refund")
    public Result<Void> rejectRefund(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return orderService.rejectRefund(SecurityUtil.requireCurrentUserId(), id, reason);
    }

    @ApiOperation("评价商家")
    @PostMapping("/{id}/rate")
    public Result<Void> rateOrder(@PathVariable Long id, @RequestParam Integer rating, @RequestParam(required = false) String comment) {
        return orderService.rateOrder(SecurityUtil.requireCurrentUserId(), id, rating, comment);
    }

    @ApiOperation("商家修改订单金额")
    @PutMapping("/{id}/modify-price")
    public Result<Void> modifyPrice(@PathVariable Long id, @RequestParam java.math.BigDecimal newPrice) {
        return orderService.modifyPrice(SecurityUtil.requireCurrentUserId(), id, newPrice);
    }

    @ApiOperation("订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetail(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("订单资金流水")
    @GetMapping("/{id}/fund-logs")
    public Result<java.util.List<FundLog>> getOrderFundLogs(@PathVariable Long id) {
        return Result.success(fundLogMapper.selectByOrderId(id));
    }

    @ApiOperation("买家订单列表")
    @GetMapping("/buyer")
    public Result<PageResult<OrderVO>> listBuyerOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderService.listBuyerOrders(SecurityUtil.requireCurrentUserId(), status, pageNum, pageSize);
    }

    @ApiOperation("卖家订单列表")
    @GetMapping("/seller")
    public Result<PageResult<OrderVO>> listSellerOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderService.listSellerOrders(SecurityUtil.requireCurrentUserId(), status, pageNum, pageSize);
    }
}
