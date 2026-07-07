package com.campustrade.mapper;

import com.campustrade.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    List<Notification> selectByUserId(@Param("userId") Long userId, @Param("isRead") Integer isRead,
                                      @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountByUserId(@Param("userId") Long userId, @Param("isRead") Integer isRead);

    Long selectUnreadCount(@Param("userId") Long userId);

    int insert(Notification notification);

    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    int markAllAsRead(@Param("userId") Long userId);

    int logicDeleteById(@Param("id") Long id, @Param("userId") Long userId);
}