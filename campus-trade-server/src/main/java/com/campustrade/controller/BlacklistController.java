package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.User;
import com.campustrade.entity.UserBlacklist;
import com.campustrade.mapper.UserBlacklistMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "黑名单接口")
@RestController
@RequestMapping("/api/blacklist")
public class BlacklistController {

    @Autowired
    private UserBlacklistMapper blacklistMapper;

    @Autowired
    private UserMapper userMapper;

    @ApiOperation("屏蔽用户")
    @PostMapping("/{blockedId}")
    public Result<Void> blockUser(@PathVariable Long blockedId) {
        Long userId = SecurityUtil.requireCurrentUserId();
        if (userId.equals(blockedId)) return Result.error(400, "不能屏蔽自己");
        UserBlacklist existing = blacklistMapper.selectByUserAndBlocked(userId, blockedId);
        if (existing != null) return Result.error(400, "已屏蔽该用户");
        UserBlacklist bl = new UserBlacklist();
        bl.setUserId(userId);
        bl.setBlockedId(blockedId);
        blacklistMapper.insert(bl);
        return Result.success();
    }

    @ApiOperation("取消屏蔽")
    @DeleteMapping("/{blockedId}")
    public Result<Void> unblockUser(@PathVariable Long blockedId) {
        Long userId = SecurityUtil.requireCurrentUserId();
        UserBlacklist existing = blacklistMapper.selectByUserAndBlocked(userId, blockedId);
        if (existing != null) blacklistMapper.deleteById(existing.getId());
        return Result.success();
    }

    @ApiOperation("屏蔽列表")
    @GetMapping
    public Result<List<Map<String, Object>>> getBlacklist() {
        Long userId = SecurityUtil.requireCurrentUserId();
        List<UserBlacklist> list = blacklistMapper.selectByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBlacklist bl : list) {
            User blocked = userMapper.selectById(bl.getBlockedId());
            if (blocked != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", bl.getId());
                map.put("blockedId", blocked.getId());
                map.put("blockedName", blocked.getNickname() != null ? blocked.getNickname() : blocked.getUsername());
                map.put("blockedAvatar", blocked.getAvatar());
                map.put("createTime", bl.getCreateTime());
                result.add(map);
            }
        }
        return Result.success(result);
    }

    @ApiOperation("是否已屏蔽")
    @GetMapping("/is-blocked/{blockedId}")
    public Result<Boolean> isBlocked(@PathVariable Long blockedId) {
        Long userId = SecurityUtil.requireCurrentUserId();
        UserBlacklist existing = blacklistMapper.selectByUserAndBlocked(userId, blockedId);
        return Result.success(existing != null);
    }
}