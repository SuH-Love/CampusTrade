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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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

    @Override
    public Result<Long> getTotalUnreadCount(Long userId) {
        Long count = chatMessageMapper.selectTotalUnreadCount(userId);
        return Result.success(count);
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
        User receiver = userMapper.selectById(msg.getReceiverId());
        if (receiver != null) {
            vo.setReceiverName(receiver.getNickname() != null ? receiver.getNickname() : receiver.getUsername());
            vo.setReceiverAvatar(receiver.getAvatar());
        }
        return vo;
    }

    private List<ChatMessageVO> toVOList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return List.of();

        Set<Long> userIds = new HashSet<>();
        for (ChatMessage msg : messages) {
            userIds.add(msg.getSenderId());
            userIds.add(msg.getReceiverId());
        }
        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
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
            User receiver = userMap.get(msg.getReceiverId());
            if (receiver != null) {
                vo.setReceiverName(receiver.getNickname() != null ? receiver.getNickname() : receiver.getUsername());
                vo.setReceiverAvatar(receiver.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Result<Void> recallMessage(Long userId, Long messageId) {
        ChatMessage msg = chatMessageMapper.selectById(messageId);
        if (msg == null) return Result.error(404, "消息不存在");
        if (!msg.getSenderId().equals(userId)) return Result.error(403, "只能撤回自己的消息");
        long minutesDiff = java.time.Duration.between(msg.getCreateTime(), java.time.LocalDateTime.now()).toMinutes();
        if (minutesDiff > 2) return Result.error(400, "超过2分钟无法撤回");
        msg.setContent("该消息已撤回");
        msg.setMessageType(4);
        chatMessageMapper.updateById(msg);
        try {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(msg.getId());
            vo.setSenderId(msg.getSenderId());
            vo.setReceiverId(msg.getReceiverId());
            vo.setContent(msg.getContent());
            vo.setMessageType(msg.getMessageType());
            vo.setCreateTime(msg.getCreateTime());
            messagingTemplate.convertAndSendToUser(String.valueOf(msg.getReceiverId()), "/queue/chat", vo);
        } catch (Exception e) {
            log.warn("STOMP push recall failed: {}", e.getMessage());
        }
        return Result.success();
    }
}