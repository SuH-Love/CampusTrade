package com.campustrade.mapper;

import com.campustrade.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiFeedbackMapper {

    int insert(AiFeedback feedback);

    List<AiFeedback> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountByUserId(@Param("userId") Long userId);

    Double selectAvgRating();
}