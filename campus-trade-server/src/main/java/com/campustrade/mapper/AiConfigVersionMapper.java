package com.campustrade.mapper;

import com.campustrade.entity.AiConfigVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiConfigVersionMapper {

    int insert(AiConfigVersion version);

    List<AiConfigVersion> selectByTypeKey(@Param("configType") String configType, @Param("configKey") String configKey);

    List<AiConfigVersion> selectByTypeId(@Param("configType") String configType, @Param("configId") Long configId);

    AiConfigVersion selectByVersion(@Param("configType") String configType, @Param("configKey") String configKey,
                                    @Param("configVersion") Integer configVersion);
}