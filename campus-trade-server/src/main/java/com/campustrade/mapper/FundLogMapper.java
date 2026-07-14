package com.campustrade.mapper;

import com.campustrade.entity.FundLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FundLogMapper {

    int insert(FundLog fundLog);

    FundLog selectById(@Param("id") Long id);

    List<FundLog> selectByOrderId(@Param("orderId") Long orderId);

    List<FundLog> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountByUserId(@Param("userId") Long userId);

    List<FundLog> selectAll(@Param("type") String type, @Param("orderId") Long orderId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountAll(@Param("type") String type, @Param("orderId") Long orderId);
}