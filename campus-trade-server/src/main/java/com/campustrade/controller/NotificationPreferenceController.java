package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.NotificationPreference;
import com.campustrade.mapper.NotificationPreferenceMapper;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "通知偏好接口")
@RestController
@RequestMapping("/api/notification-preference")
public class NotificationPreferenceController {
    @Autowired
    private NotificationPreferenceMapper preferenceMapper;

    @ApiOperation("获取我的通知偏好")
    @GetMapping
    public Result<List<NotificationPreference>> getMyPreferences() {
        return Result.success(preferenceMapper.selectByUserId(SecurityUtil.requireCurrentUserId()));
    }

    @ApiOperation("设置通知偏好")
    @PutMapping("/{type}")
    public Result<Void> setPreference(@PathVariable String type, @RequestParam Integer enabled) {
        Long userId = SecurityUtil.requireCurrentUserId();
        NotificationPreference existing = preferenceMapper.selectByUserAndType(userId, type);
        if (existing != null) {
            preferenceMapper.updateEnabled(existing.getId(), enabled);
        } else {
            NotificationPreference pref = new NotificationPreference();
            pref.setUserId(userId);
            pref.setNotificationType(type);
            pref.setEnabled(enabled);
            preferenceMapper.insert(pref);
        }
        return Result.success();
    }
}