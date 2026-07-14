package com.campustrade.mapper;

import com.campustrade.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemConfigMapper {

    List<SystemConfig> selectAll();

    SystemConfig selectByKey(@Param("configKey") String configKey);

    int insert(SystemConfig config);

    int updateByKey(SystemConfig config);
}