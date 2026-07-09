package com.campustrade.config;

import com.campustrade.security.StompPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StompEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private static final Set<Long> ONLINE_USERS = ConcurrentHashMap.newKeySet();

    public StompEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompPrincipal principal = extractPrincipal(event);
        if (principal != null) {
            ONLINE_USERS.add(principal.getUserId());
            broadcastOnlineStatus(principal.getUserId(), true);
            log.info("STOMP online: userId={}, onlineCount={}", principal.getUserId(), ONLINE_USERS.size());
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompPrincipal principal = extractPrincipal(event);
        if (principal != null) {
            ONLINE_USERS.remove(principal.getUserId());
            broadcastOnlineStatus(principal.getUserId(), false);
            log.info("STOMP offline: userId={}, onlineCount={}", principal.getUserId(), ONLINE_USERS.size());
        }
    }

    private void broadcastOnlineStatus(Long userId, boolean online) {
        messagingTemplate.convertAndSend("/topic/online",
                Map.of("type", "ONLINE_STATUS", "userId", userId, "online", online));
    }

    private StompPrincipal extractPrincipal(Object event) {
        try {
            var message = event instanceof SessionConnectEvent
                    ? ((SessionConnectEvent) event).getMessage()
                    : ((SessionDisconnectEvent) event).getMessage();
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            var user = accessor.getUser();
            if (user instanceof StompPrincipal) return (StompPrincipal) user;
            if (user instanceof org.springframework.security.core.Authentication) {
                var principal = ((org.springframework.security.core.Authentication) user).getPrincipal();
                if (principal instanceof StompPrincipal) return (StompPrincipal) principal;
            }
            var simpUser = message.getHeaders().get("simpUser");
            if (simpUser instanceof StompPrincipal) return (StompPrincipal) simpUser;
            if (simpUser instanceof org.springframework.security.core.Authentication) {
                var details = ((org.springframework.security.core.Authentication) simpUser).getPrincipal();
                if (details instanceof StompPrincipal) return (StompPrincipal) details;
            }
        } catch (Exception e) {
            log.debug("Extract principal failed: {}", e.getMessage());
        }
        return null;
    }

    public static boolean isOnline(Long userId) {
        return ONLINE_USERS.contains(userId);
    }

    public static Set<Long> getOnlineUserIds() {
        return Set.copyOf(ONLINE_USERS);
    }
}