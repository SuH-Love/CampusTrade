package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.dto.PasswordUpdateDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.UserService;
import com.campustrade.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Result<UserVO> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(toVO(user));
    }

    @Override
    public Result<UserVO> getUserPublicInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRealVerified(user.getRealVerified());
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<UserVO> updateUserInfo(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        if (dto.getPhone() != null) {
            User phoneUser = userMapper.selectByPhone(dto.getPhone());
            if (phoneUser != null && !phoneUser.getId().equals(userId)) {
                return Result.error(ResultCode.PHONE_EXISTS);
            }
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            User emailUser = userMapper.selectByEmail(dto.getEmail());
            if (emailUser != null && !emailUser.getId().equals(userId)) {
                return Result.error(ResultCode.EMAIL_EXISTS);
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getStudentId() != null) user.setStudentId(dto.getStudentId());
        userMapper.updateById(user);
        return Result.success(toVO(user));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updatePassword(Long userId, PasswordUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.error(ResultCode.OLD_PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> uploadAvatar(Long userId, String fileUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        user.setAvatar(fileUrl);
        userMapper.updateById(user);
        return Result.success(fileUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> realNameVerify(Long userId, String realName, String studentId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        user.setRealName(realName);
        user.setStudentId(studentId);
        user.setRealVerified(1);
        userMapper.updateById(user);
        return Result.success();
    }

    @Override
    public Result<PageResult<UserVO>> listUsers(String username, Integer status, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<User> users = userMapper.selectList(username, status, offset, pageSize);
        Long total = userMapper.selectCount(username, status);
        List<UserVO> vos = users.stream().map(u -> toVO(u, true)).collect(Collectors.toList());
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> banUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error(ResultCode.NOT_FOUND);
        user.setStatus(0);
        userMapper.updateById(user);
        redisTemplate.opsForValue().set("ban:user:" + userId, "1", 7, java.util.concurrent.TimeUnit.DAYS);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error(ResultCode.NOT_FOUND);
        user.setStatus(1);
        userMapper.updateById(user);
        redisTemplate.delete("ban:user:" + userId);
        return Result.success();
    }

    @Override
    public long countUsers() {
        return userMapper.selectCount(null, null);
    }

    private UserVO toVO(User user) {
        return toVO(user, false);
    }

    private UserVO toVO(User user, boolean maskSensitive) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(maskSensitive ? maskPhone(user.getPhone()) : user.getPhone());
        vo.setEmail(maskSensitive ? maskEmail(user.getEmail()) : user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setRealName(maskSensitive ? maskName(user.getRealName()) : user.getRealName());
        vo.setStudentId(maskSensitive ? maskStudentId(user.getStudentId()) : user.getStudentId());
        vo.setRealVerified(user.getRealVerified());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        return name.charAt(0) + "**";
    }

    private String maskStudentId(String id) {
        if (id == null || id.length() < 4) return id;
        return id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }
}