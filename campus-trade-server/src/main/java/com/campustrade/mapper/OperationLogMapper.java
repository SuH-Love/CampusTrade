package com.campustrade.mapper;

import com.campustrade.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    List<OperationLog> selectList(@Param("module") String module, @Param("username") String username,
                                  @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCount(@Param("module") String module, @Param("username") String username);

    int insert(OperationLog log);
}