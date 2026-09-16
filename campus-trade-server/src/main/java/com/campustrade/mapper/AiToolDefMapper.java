package com.campustrade.mapper;

import com.campustrade.entity.AiToolDef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiToolDefMapper {

    int insert(AiToolDef toolDef);

    AiToolDef selectByName(@Param("toolName") String toolName);

    List<AiToolDef> selectAllActive();

    List<AiToolDef> selectByGroup(@Param("toolGroup") String toolGroup);

    int update(AiToolDef toolDef);

    int toggleActive(@Param("toolName") String toolName, @Param("isActive") Integer isActive);

    int count();
}