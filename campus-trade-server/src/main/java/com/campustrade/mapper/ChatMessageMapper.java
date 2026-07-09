package com.campustrade.mapper;

import com.campustrade.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    List<ChatMessage> selectHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2,
                                    @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    List<ChatMessage> selectRecentContacts(@Param("userId") Long userId);

    Long selectUnreadCount(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    int markAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    int insert(ChatMessage message);

    int insertBatch(@Param("messages") List<ChatMessage> messages);

    int updateById(ChatMessage message);

    Long selectTotalUnreadCount(@Param("receiverId") Long receiverId);
}