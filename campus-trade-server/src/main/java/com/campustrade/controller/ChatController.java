package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.ChatSendDTO;
import com.campustrade.service.ChatService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.ChatMessageVO;
import com.campustrade.websocket.ChatWebSocketHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Api(tags = "聊天接口")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @ApiOperation("发送消息")
    @PostMapping
    public Result<Void> sendMessage(@Validated @RequestBody ChatSendDTO dto) {
        return chatService.sendMessage(SecurityUtil.requireCurrentUserId(), dto);
    }

    @ApiOperation("聊天记录")
    @GetMapping("/history/{targetUserId}")
    public Result<PageResult<ChatMessageVO>> getHistory(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return chatService.getHistory(SecurityUtil.requireCurrentUserId(), targetUserId, pageNum, pageSize);
    }

    @ApiOperation("最近会话")
    @GetMapping("/recent")
    public Result<PageResult<ChatMessageVO>> getRecentContacts() {
        return chatService.getRecentContacts(SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("未读数量")
    @GetMapping("/unread/{senderId}")
    public Result<Long> getUnreadCount(@PathVariable Long senderId) {
        return chatService.getUnreadCount(SecurityUtil.requireCurrentUserId(), senderId);
    }

    @ApiOperation("标记已读")
    @PutMapping("/read/{senderId}")
    public Result<Void> markAsRead(@PathVariable Long senderId) {
        return chatService.markAsRead(SecurityUtil.requireCurrentUserId(), senderId);
    }

    @ApiOperation("在线用户列表")
    @GetMapping("/online-users")
    public Result<List<Long>> getOnlineUsers() {
        Set<Long> onlineUserIds = ChatWebSocketHandler.getOnlineUserIds();
        return Result.success(onlineUserIds.stream().collect(Collectors.toList()));
    }

    @ApiOperation("总未读消息数")
    @GetMapping("/unread-total")
    public Result<Long> getTotalUnreadCount() {
        return chatService.getTotalUnreadCount(SecurityUtil.requireCurrentUserId());
    }
}
