package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.service.NotificationService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.NotificationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "通知接口")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @ApiOperation("通知列表")
    @GetMapping
    public Result<PageResult<NotificationVO>> listNotifications(
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return notificationService.listNotifications(SecurityUtil.requireCurrentUserId(), isRead, pageNum, pageSize);
    }

    @ApiOperation("未读数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        return notificationService.getUnreadCount(SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("标记已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("全部已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        return notificationService.markAllAsRead(SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        return notificationService.deleteNotification(SecurityUtil.requireCurrentUserId(), id);
    }
}
