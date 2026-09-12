package com.campustrade.mapper;

import com.campustrade.entity.AiKnowledge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiKnowledgeMapper {

    int insert(AiKnowledge knowledge);

    int update(AiKnowledge knowledge);

    int deleteById(Long id);

    List<AiKnowledge> selectAllEnabled();

    List<AiKnowledge> selectAll();

    AiKnowledge selectById(Long id);
}