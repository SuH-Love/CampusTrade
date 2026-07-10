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
                                @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountByBuyerId(@Param("buyerId") Long buyerId, @Param("status") String status);

    List<Order> selectBySellerId(@Param("sellerId") Long sellerId, @Param("status") String status,
                                 @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountBySellerId(@Param("sellerId") Long sellerId, @Param("status") String status);

    int insert(Order order);

    int updateById(Order order);

    List<Order> selectAll(@Param("status") String status,
                          @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountAll(@Param("status") String status);

    Long selectCountByStatus(@Param("status") String status);

    Long selectCountToday();

    List<Order> selectTimeoutPendingPay(@Param("timeout") java.time.LocalDateTime timeout);
}