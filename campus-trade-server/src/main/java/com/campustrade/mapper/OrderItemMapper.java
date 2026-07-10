package com.campustrade.mapper;

import com.campustrade.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    int insertBatch(@Param("items") List<OrderItem> items);

    int updatePriceById(@Param("id") Long id, @Param("price") java.math.BigDecimal price);
}