package com.campustrade.mapper;

import com.campustrade.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiFeedbackMapper {

    int insert(AiFeedback feedback);

    List<AiFeedback> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountByUserId(@Param("userId") Long userId);

    Double selectAvgRating();

    List<AiFeedback> selectBySessionAndUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    AiFeedback selectByUserSessionAiResponse(@Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("aiResponse") String aiResponse);

    int updateRating(@Param("id") Long id, @Param("rating") Integer rating, @Param("feedback") String feedback);

    int deleteByUserSessionAiResponse(@Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("aiResponse") String aiResponse);

    List<String> selectRecentUserMessages(@Param("limit") int limit);

    List<AiFeedback> selectAllForRlhf(@Param("offset") Integer offset, @Param("limit") Integer limit);

    Integer countByRating(@Param("minRating") int minRating, @Param("maxRating") int maxRating);

    List<Map<String, Object>> countByDate(@Param("days") int days);
}