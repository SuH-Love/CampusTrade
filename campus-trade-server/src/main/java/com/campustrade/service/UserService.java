package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.PasswordUpdateDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.vo.UserVO;

public interface UserService {

    Result<UserVO> getUserInfo(Long userId);

    Result<UserVO> getUserPublicInfo(Long userId);

    Result<UserVO> updateUserInfo(Long userId, UserUpdateDTO dto);

    Result<Void> updatePassword(Long userId, PasswordUpdateDTO dto);

    Result<String> uploadAvatar(Long userId, String fileUrl);

    Result<Void> realNameVerify(Long userId, String realName, String studentId);

    Result<PageResult<UserVO>> listUsers(String username, Integer status, Integer pageNum, Integer pageSize);

    Result<Void> banUser(Long userId);

    Result<Void> unbanUser(Long userId);

    long countUsers();
}