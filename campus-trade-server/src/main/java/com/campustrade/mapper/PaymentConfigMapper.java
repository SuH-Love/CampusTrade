package com.campustrade.mapper;

import com.campustrade.entity.PaymentConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentConfigMapper {

    PaymentConfig selectById(@Param("id") Long id);

    List<PaymentConfig> selectByUserId(@Param("userId") Long userId);

    PaymentConfig selectDefaultByUserId(@Param("userId") Long userId);

    int insert(PaymentConfig config);

    int updateById(PaymentConfig config);

    int logicDeleteById(@Param("id") Long id, @Param("userId") Long userId);

    int resetDefaultByUserId(@Param("userId") Long userId);
}