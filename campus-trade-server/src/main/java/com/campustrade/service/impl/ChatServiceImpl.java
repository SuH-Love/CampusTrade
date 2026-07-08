package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.constant.MQConstant;
import com.campustrade.constant.RedisConstant;
import com.campustrade.dto.ChatSendDTO;
import com.campustrade.entity.ChatMessage;
import com.campustrade.entity.User;
import com.campustrade.mapper.ChatMessageMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.ChatService;
import com.campustrade.vo.ChatMessageVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result<Void> sendMessage(Long senderId, ChatSendDTO dto) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : 1);
        message.setIsRead(0);
        chatMessageMapper.insert(message);
        return Result.success();
    }

    @Override
    public Result<PageResult<ChatMessageVO>> getHistory(Long userId, Long targetUserId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<ChatMessage> list = chatMessageMapper.selectHistory(userId, targetUserId, offset, pageSize);
        List<ChatMessageVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, (long) vos.size()));
    }

    @Override
    public Result<PageResult<ChatMessageVO>> getRecentContacts(Long userId) {
        List<ChatMessage> list = chatMessageMapper.selectRecentContacts(userId);
        List<ChatMessageVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, (long) vos.size()));
    }

    @Override
    public Result<Long> getUnreadCount(Long userId, Long senderId) {
        Long count = chatMessageMapper.selectUnreadCount(userId, senderId);
        return Result.success(count);
    }

    @Override
    public Result<Void> markAsRead(Long userId, Long senderId) {
        chatMessageMapper.markAsRead(userId, senderId);
        return Result.success();
    }

    private ChatMessageVO toVO(ChatMessage msg) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(msg.getId());
        vo.setSenderId(msg.getSenderId());
        vo.setReceiverId(msg.getReceiverId());
        vo.setContent(msg.getContent());
        vo.setMessageType(msg.getMessageType());
        vo.setIsRead(msg.getIsRead());
        vo.setCreateTime(msg.getCreateTime());
        User sender = userMapper.selectById(msg.getSenderId());
        if (sender != null) {
            vo.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
            vo.setSenderAvatar(sender.getAvatar());
        }
        return vo;
    }

    private List<ChatMessageVO> toVOList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return List.of();

        List<Long> senderIds = messages.stream().map(ChatMessage::getSenderId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectByIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return messages.stream().map(msg -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(msg.getId());
            vo.setSenderId(msg.getSenderId());
            vo.setReceiverId(msg.getReceiverId());
            vo.setContent(msg.getContent());
            vo.setMessageType(msg.getMessageType());
            vo.setIsRead(msg.getIsRead());
            vo.setCreateTime(msg.getCreateTime());
            User sender = userMap.get(msg.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
                vo.setSenderAvatar(sender.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}