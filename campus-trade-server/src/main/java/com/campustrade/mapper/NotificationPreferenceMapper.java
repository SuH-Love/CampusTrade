package com.campustrade.mapper;

import com.campustrade.entity.NotificationPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationPreferenceMapper {
    NotificationPreference selectByUserAndType(@Param("userId") Long userId, @Param("type") String type);
    List<NotificationPreference> selectByUserId(@Param("userId") Long userId);
    int insert(NotificationPreference pref);
    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);
}