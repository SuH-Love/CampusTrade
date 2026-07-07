package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.ChatSendDTO;
import com.campustrade.vo.ChatMessageVO;

public interface ChatService {

    Result<Void> sendMessage(Long senderId, ChatSendDTO dto);

    Result<PageResult<ChatMessageVO>> getHistory(Long userId, Long targetUserId, Integer pageNum, Integer pageSize);

    Result<PageResult<ChatMessageVO>> getRecentContacts(Long userId);

    Result<Long> getUnreadCount(Long userId, Long senderId);

    Result<Void> markAsRead(Long userId, Long senderId);
}