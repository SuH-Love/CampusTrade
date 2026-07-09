package com.campustrade.mapper;

import com.campustrade.entity.SellerRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SellerRatingMapper {
    SellerRating selectById(@Param("id") Long id);
    SellerRating selectByOrderId(@Param("orderId") Long orderId);
    List<SellerRating> selectBySellerId(@Param("sellerId") Long sellerId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);
    Long selectCountBySellerId(@Param("sellerId") Long sellerId);
    Double selectAvgRatingBySellerId(@Param("sellerId") Long sellerId);
    int insert(SellerRating rating);
}