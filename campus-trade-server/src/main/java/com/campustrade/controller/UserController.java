package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.dto.PasswordUpdateDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.service.UserService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "用户接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @ApiOperation("获取个人信息")
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        return userService.getUserInfo(SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("获取用户公开信息")
    @GetMapping("/{id}")
    public Result<UserVO> getUserPublicInfo(@PathVariable Long id) {
        return userService.getUserPublicInfo(id);
    }

    @ApiOperation("修改个人信息")
    @PutMapping("/info")
    public Result<UserVO> updateUserInfo(@Validated @RequestBody UserUpdateDTO dto) {
        return userService.updateUserInfo(SecurityUtil.requireCurrentUserId(), dto);
    }

    @ApiOperation("修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Validated @RequestBody PasswordUpdateDTO dto) {
        return userService.updatePassword(SecurityUtil.requireCurrentUserId(), dto);
    }

    @ApiOperation("实名认证")
    @PostMapping("/verify")
    public Result<Void> realNameVerify(@RequestParam String realName, @RequestParam String studentId) {
        return userService.realNameVerify(SecurityUtil.requireCurrentUserId(), realName, studentId);
    }

    @ApiOperation("上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam String fileUrl) {
        return userService.uploadAvatar(SecurityUtil.requireCurrentUserId(), fileUrl);
    }

    @ApiOperation("用户统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        Long userId = SecurityUtil.requireCurrentUserId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("publishedGoods", goodsMapper.selectCount(null, null, null, null, null, userId));
        stats.put("onlineGoods", goodsMapper.selectCount(null, null, null, null, "ONLINE", userId));
        stats.put("buyerOrders", orderMapper.selectCountByBuyerId(userId, null));
        stats.put("sellerOrders", orderMapper.selectCountBySellerId(userId, null));
        stats.put("finishedOrders", orderMapper.selectCountByBuyerId(userId, "FINISHED"));
        stats.put("totalSpent", orderMapper.selectTotalSpentByBuyerId(userId));
        stats.put("totalEarned", orderMapper.selectTotalEarnedBySellerId(userId));
        return Result.success(stats);
    }
}