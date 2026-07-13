package com.campustrade.mapper;

import com.campustrade.entity.SecurityLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecurityLogMapper {

    List<SecurityLog> selectList(@Param("keyword") String keyword,
                                 @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCount(@Param("keyword") String keyword);

    Long selectCountTodayByEventType(@Param("eventType") String eventType);

    int insert(SecurityLog log);
}