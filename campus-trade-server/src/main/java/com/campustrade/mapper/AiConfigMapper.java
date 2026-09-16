package com.campustrade.mapper;

import com.campustrade.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiConfigMapper {

    int insert(AiConfig config);

    AiConfig selectByGroupKey(@Param("configGroup") String configGroup, @Param("configKey") String configKey);

    List<AiConfig> selectAllActive();

    List<AiConfig> selectByGroup(@Param("configGroup") String configGroup);

    int update(@Param("configGroup") String configGroup, @Param("configKey") String configKey,
               @Param("configValue") String configValue, @Param("updatedBy") Long updatedBy);

    int count();
}