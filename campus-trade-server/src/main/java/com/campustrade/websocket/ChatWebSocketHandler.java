package com.campustrade.websocket;

import com.campustrade.dto.WsChatMessage;
import com.campustrade.entity.ChatMessage;
import com.campustrade.entity.User;
import com.campustrade.mapper.ChatMessageMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.vo.ChatMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;

    public ChatWebSocketHandler(ChatMessageMapper chatMessageMapper, UserMapper userMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            ONLINE_USERS.put(userId, session);
            broadcastOnlineStatus(userId, true);
            log.info("WebSocket connected: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Long senderId = getUserId(session);
        if (senderId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        try {
            WsChatMessage wsMsg = objectMapper.readValue(textMessage.getPayload(), WsChatMessage.class);
            String type = wsMsg.getType();

            if ("CHAT".equals(type)) {
                handleChatMessage(senderId, wsMsg);
            } else if ("PING".equals(type)) {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            } else if ("TYPING".equals(type)) {
                WebSocketSession receiverSession = ONLINE_USERS.get(wsMsg.getReceiverId());
                if (receiverSession != null && receiverSession.isOpen()) {
                    receiverSession.sendMessage(new TextMessage(
                            objectMapper.writeValueAsString(Map.of("type", "TYPING", "userId", senderId))));
                }
            }
        } catch (Exception e) {
            log.error("WebSocket message handling error: userId={}", senderId, e);
        }
    }

    private void handleChatMessage(Long senderId, WsChatMessage wsMsg) throws Exception {
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

        String json = objectMapper.writeValueAsString(Map.of("type", "CHAT", "data", vo));

        WebSocketSession receiverSession = ONLINE_USERS.get(wsMsg.getReceiverId());
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(json));
            message.setIsRead(1);
            chatMessageMapper.updateById(message);
        }

        WebSocketSession senderSession = ONLINE_USERS.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            ONLINE_USERS.remove(userId);
            broadcastOnlineStatus(userId, false);
            log.info("WebSocket disconnected: userId={}, status={}", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            ONLINE_USERS.remove(userId);
            broadcastOnlineStatus(userId, false);
        }
        log.warn("WebSocket transport error: userId={}", userId, exception);
    }

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? (Long) userId : null;
    }

    private ChatMessageVO buildMessageVO(ChatMessage msg) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(msg.getId());
        vo.setSenderId(msg.getSenderId());
        vo.setReceiverId(msg.getReceiverId());
        vo.setContent(msg.getContent());
        vo.setMessageType(msg.getMessageType());
        vo.setIsRead(msg.getIsRead());
        vo.setCreateTime(msg.getCreateTime() != null ? msg.getCreateTime() : LocalDateTime.now());

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

    private void broadcastOnlineStatus(Long userId, boolean online) {
        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of("type", "ONLINE_STATUS", "userId", userId, "online", online));
        } catch (Exception e) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (Map.Entry<Long, WebSocketSession> entry : ONLINE_USERS.entrySet()) {
            if (!entry.getKey().equals(userId) && entry.getValue().isOpen()) {
                try {
                    entry.getValue().sendMessage(msg);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static boolean isOnline(Long userId) {
        return ONLINE_USERS.containsKey(userId);
    }

    public static int getOnlineCount() {
        return ONLINE_USERS.size();
    }

    public static Set<Long> getOnlineUserIds() {
        return new HashSet<>(ONLINE_USERS.keySet());
    }
}