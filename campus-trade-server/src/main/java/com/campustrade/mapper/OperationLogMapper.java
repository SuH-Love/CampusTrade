package com.campustrade.mapper;

import com.campustrade.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    List<OperationLog> selectList(@Param("keyword") String keyword,
                                  @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCount(@Param("keyword") String keyword);

    int insert(OperationLog log);
}