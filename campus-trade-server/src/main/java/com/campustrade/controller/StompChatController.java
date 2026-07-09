package com.campustrade.controller;

import com.campustrade.dto.WsChatMessage;
import com.campustrade.entity.ChatMessage;
import com.campustrade.entity.User;
import com.campustrade.mapper.ChatMessageMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.StompPrincipal;
import com.campustrade.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
public class StompChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @MessageMapping("/chat.send")
    public void handleChat(@Payload WsChatMessage wsMsg, Principal principal) {
        if (!(principal instanceof StompPrincipal)) return;
        Long senderId = ((StompPrincipal) principal).getUserId();

        if (wsMsg.getReceiverId() == null || wsMsg.getContent() == null || wsMsg.getContent().trim().isEmpty()) {
            return;
        }

        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(wsMsg.getReceiverId());
        message.setContent(wsMsg.getContent().trim());
        message.setMessageType(wsMsg.getMessageType() != null ? wsMsg.getMessageType() : 1);
        message.setIsRead(0);
        chatMessageMapper.insert(message);

        ChatMessageVO vo = buildMessageVO(message);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(wsMsg.getReceiverId()), "/queue/chat", vo);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderId), "/queue/chat", vo);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        if (!(principal instanceof StompPrincipal)) return;
        Long senderId = ((StompPrincipal) principal).getUserId();
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId), "/queue/chat",
                Map.of("type", "TYPING", "userId", senderId));
    }

    @MessageMapping("/chat.stopTyping")
    public void handleStopTyping(@Payload Map<String, Object> payload, Principal principal) {
        if (!(principal instanceof StompPrincipal)) return;
        Long senderId = ((StompPrincipal) principal).getUserId();
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId), "/queue/chat",
                Map.of("type", "STOP_TYPING", "userId", senderId));
    }

    @MessageMapping("/chat.read")
    public void handleRead(@Payload Map<String, Object> payload, Principal principal) {
        if (!(principal instanceof StompPrincipal)) return;
        Long senderId = ((StompPrincipal) principal).getUserId();
        Long partnerId = Long.valueOf(payload.get("receiverId").toString());
        chatMessageMapper.markAsRead(senderId, partnerId);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(partnerId), "/queue/chat",
                Map.of("type", "READ", "userId", senderId));
    }

    @SubscribeMapping("/queue/chat")
    public void onSubscribe(Principal principal) {
        if (principal instanceof StompPrincipal) {
            log.info("User subscribed to chat queue: userId={}", ((StompPrincipal) principal).getUserId());
        }
    }

    private ChatMessageVO buildMessageVO(ChatMessage msg) {
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
}