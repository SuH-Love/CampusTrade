package com.campustrade.mapper;

import com.campustrade.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    Order selectById(@Param("id") Long id);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    List<Order> selectByBuyerId(@Param("buyerId") Long buyerId, @Param("status") String status,
                                @Param("offset") Integer offset, @Param("pageSize") Integer pageSize,
                                @Param("startDate") String startDate, @Param("endDate") String endDate);

    Long selectCountByBuyerId(@Param("buyerId") Long buyerId, @Param("status") String status,
                              @Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Order> selectBySellerId(@Param("sellerId") Long sellerId, @Param("status") String status,
                                 @Param("offset") Integer offset, @Param("pageSize") Integer pageSize,
                                 @Param("startDate") String startDate, @Param("endDate") String endDate);

    Long selectCountBySellerId(@Param("sellerId") Long sellerId, @Param("status") String status,
                               @Param("startDate") String startDate, @Param("endDate") String endDate);

    int insert(Order order);

    int updateById(Order order);

    List<Order> selectAll(@Param("orderNo") String orderNo, @Param("status") String status,
                          @Param("startDate") String startDate, @Param("endDate") String endDate,
                          @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountAll(@Param("orderNo") String orderNo, @Param("status") String status,
                        @Param("startDate") String startDate, @Param("endDate") String endDate);

    Long selectCountByStatus(@Param("status") String status);

    Long selectCountToday();

    List<Order> selectTimeoutPendingPay(@Param("timeout") java.time.LocalDateTime timeout);

    java.math.BigDecimal selectTotalSpentByBuyerId(@Param("buyerId") Long buyerId);

    java.math.BigDecimal selectTotalEarnedBySellerId(@Param("sellerId") Long sellerId);
}