package com.campustrade.mapper;

import com.campustrade.entity.AiPromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPromptTemplateMapper {

    int insert(AiPromptTemplate template);

    AiPromptTemplate selectByKey(@Param("templateKey") String templateKey);

    AiPromptTemplate selectByKeyForUpdate(@Param("templateKey") String templateKey);

    List<AiPromptTemplate> selectAllActive();

    List<AiPromptTemplate> selectByCategory(@Param("category") String category);

    int updateContent(@Param("templateKey") String templateKey, @Param("content") String content,
                      @Param("configVersion") Integer configVersion, @Param("updatedBy") Long updatedBy,
                      @Param("oldVersion") Integer oldVersion);

    int count();
}