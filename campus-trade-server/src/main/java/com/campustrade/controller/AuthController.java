package com.campustrade.controller;

import com.campustrade.aspect.RateLimit;
import com.campustrade.aspect.RepeatSubmit;
import com.campustrade.common.Result;
import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.RefreshTokenDTO;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.dto.SendCodeDTO;
import com.campustrade.dto.ResetPasswordDTO;
import com.campustrade.service.AuthService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.TokenVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @ApiOperation("注册")
    @PostMapping("/register")
    @RateLimit
    @RepeatSubmit
    public Result<TokenVO> register(@Validated @RequestBody RegisterDTO dto) {
        return authService.register(dto);
    }

    @ApiOperation("登录")
    @PostMapping("/login")
    @RateLimit
    public Result<TokenVO> login(@Validated @RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    @ApiOperation("退出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return authService.logout(SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("刷新Token")
    @PostMapping("/refresh")
    public Result<TokenVO> refreshToken(@Validated @RequestBody RefreshTokenDTO dto) {
        return authService.refreshToken(dto);
    }

    @ApiOperation("重置密码")
    @PostMapping("/reset-password")
    @RateLimit
    public Result<Void> resetPassword(@Validated @RequestBody ResetPasswordDTO dto) {
        return authService.resetPassword(dto);
    }

    @ApiOperation("发送重置密码验证码")
    @PostMapping("/send-code")
    @RateLimit
    public Result<Void> sendResetCode(@Validated @RequestBody SendCodeDTO dto) {
        return authService.sendResetCode(dto);
    }

}