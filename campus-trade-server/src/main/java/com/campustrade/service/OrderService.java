package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.OrderCreateDTO;
import com.campustrade.vo.OrderVO;

public interface OrderService {

    Result<OrderVO> createOrder(Long buyerId, OrderCreateDTO dto);

    Result<Void> cancelOrder(Long userId, Long orderId, String reason);

    Result<Void> payOrder(Long userId, Long orderId);

    Result<Void> shipOrder(Long userId, Long orderId, String trackingNo);

    Result<Void> finishOrder(Long userId, Long orderId);

    Result<Void> refundOrder(Long userId, Long orderId, String reason);

    Result<Void> approveRefund(Long userId, Long orderId);

    Result<Void> rejectRefund(Long userId, Long orderId, String reason);

    Result<Void> rateOrder(Long userId, Long orderId, Integer rating, String comment);

    Result<Void> modifyPrice(Long sellerId, Long orderId, java.math.BigDecimal newPrice);

    Result<Void> adminApproveRefund(Long orderId);

    Result<Void> adminRejectRefund(Long orderId, String reason);

    Result<OrderVO> getOrderDetail(Long userId, Long orderId);

    Result<PageResult<OrderVO>> listBuyerOrders(Long buyerId, String status, Integer pageNum, Integer pageSize);

    Result<PageResult<OrderVO>> listSellerOrders(Long sellerId, String status, Integer pageNum, Integer pageSize);

    Result<PageResult<OrderVO>> listOrdersByAdmin(String status, Integer pageNum, Integer pageSize);

    Result<PageResult<OrderVO>> listAllOrders(String status, Integer pageNum, Integer pageSize);

    long countOrders();
}