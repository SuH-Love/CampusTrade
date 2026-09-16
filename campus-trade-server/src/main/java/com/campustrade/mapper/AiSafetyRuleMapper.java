package com.campustrade.mapper;

import com.campustrade.entity.AiSafetyRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiSafetyRuleMapper {

    int insert(AiSafetyRule rule);

    List<AiSafetyRule> selectByType(@Param("ruleType") String ruleType);

    List<AiSafetyRule> selectAllActive();

    int update(AiSafetyRule rule);

    int deleteById(@Param("id") Long id);

    int toggleActive(@Param("id") Long id, @Param("isActive") Integer isActive);

    int count();
}