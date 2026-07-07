package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.OrderCreateDTO;
import com.campustrade.service.OrderService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "订单接口")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

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

    @ApiOperation("支付")
    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id) {
        return orderService.payOrder(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("发货")
    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(@PathVariable Long id) {
        return orderService.shipOrder(SecurityUtil.requireCurrentUserId(), id);
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

    @ApiOperation("订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetail(SecurityUtil.requireCurrentUserId(), id);
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
