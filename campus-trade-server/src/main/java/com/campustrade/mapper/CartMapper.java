package com.campustrade.mapper;

import com.campustrade.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {
    List<Cart> selectByUserId(@Param("userId") Long userId);
    Cart selectByUserAndGoods(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
    Cart selectById(@Param("id") Long id);
    int insert(Cart cart);
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);
    int deleteById(@Param("id") Long id);
    int deleteByUserId(@Param("userId") Long userId);
    int restoreByUserAndGoods(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}