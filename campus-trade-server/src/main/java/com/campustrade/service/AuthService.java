package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.*;
import com.campustrade.vo.TokenVO;
import com.campustrade.vo.UserVO;

public interface AuthService {

    Result<TokenVO> register(RegisterDTO dto);

    Result<TokenVO> login(LoginDTO dto);

    Result<Void> logout(Long userId);

    Result<TokenVO> refreshToken(RefreshTokenDTO dto);

    Result<Void> resetPassword(String username, String phone, String newPassword);

    Result<Void> sendResetCode(SendCodeDTO dto);

    Result<Void> resetPassword(ResetPasswordDTO dto);
}