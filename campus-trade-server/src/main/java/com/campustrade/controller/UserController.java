package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.dto.PasswordUpdateDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.service.UserService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "用户接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @ApiOperation("获取个人信息")
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        return userService.getUserInfo(SecurityUtil.requireCurrentUserId());
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
}