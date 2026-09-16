package com.campustrade.mapper;

import com.campustrade.entity.AiQuickQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiQuickQuestionMapper {

    int insert(AiQuickQuestion question);

    List<AiQuickQuestion> selectAllActive();

    List<AiQuickQuestion> selectByCategory(@Param("category") String category);

    int update(AiQuickQuestion question);

    int deleteById(@Param("id") Long id);

    int toggleActive(@Param("id") Long id, @Param("isActive") Integer isActive);

    int count();
}