package com.campustrade.mapper;

import com.campustrade.entity.DeliveryAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeliveryAddressMapper {

    List<DeliveryAddress> selectByUserId(@Param("userId") Long userId);

    DeliveryAddress selectById(@Param("id") Long id);

    int insert(DeliveryAddress address);

    int updateById(DeliveryAddress address);

    int deleteById(@Param("id") Long id);

    int resetDefaultByUserId(@Param("userId") Long userId);

    DeliveryAddress selectDefaultByUserId(@Param("userId") Long userId);
}