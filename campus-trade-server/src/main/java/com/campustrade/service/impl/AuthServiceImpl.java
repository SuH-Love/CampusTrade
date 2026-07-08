package com.campustrade.service.impl;

import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.constant.AccountLockConstant;
import com.campustrade.constant.RedisConstant;
import com.campustrade.constant.SecurityConstant;
import com.campustrade.dto.*;
import com.campustrade.entity.User;
import com.campustrade.entity.Role;
import com.campustrade.entity.UserRole;
import com.campustrade.enum_.SecurityEventType;
import com.campustrade.mapper.UserMapper;
import com.campustrade.mapper.RoleMapper;
import com.campustrade.mapper.PermissionMapper;
import com.campustrade.mapper.UserRoleMapper;
import com.campustrade.service.AuthService;
import com.campustrade.service.LogService;
import com.campustrade.entity.SecurityLog;
import com.campustrade.util.IpUtil;
import com.campustrade.util.JwtUtil;
import com.campustrade.util.PasswordUtil;
import com.campustrade.util.SensitiveWordUtil;
import com.campustrade.vo.TokenVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private LogService logService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TokenVO> register(RegisterDTO dto) {
        String ip = IpUtil.getIpAddr();
        String registerLimitKey = AccountLockConstant.REGISTER_LIMIT_PREFIX + ip;
        Long registerCount = redisTemplate.opsForValue().increment(registerLimitKey);
        if (registerCount != null && registerCount == 1) {
            redisTemplate.expire(registerLimitKey, AccountLockConstant.REGISTER_LIMIT_TTL, TimeUnit.SECONDS);
        }
        if (registerCount != null && registerCount > AccountLockConstant.MAX_REGISTER_PER_IP) {
            logService.recordSecurityLog(buildSecurityLog(null, dto.getUsername(), SecurityEventType.RATE_LIMIT.getCode(), ip, "注册频率过高"));
            return Result.error(ResultCode.RATE_LIMIT_EXCEEDED);
        }

        if (!PasswordUtil.isStrongPassword(dto.getPassword())) {
            return Result.error(400, PasswordUtil.getPasswordRequirement());
        }

        if (SensitiveWordUtil.containsSensitiveWord(dto.getUsername())) {
            return Result.error(400, "用户名包含敏感词");
        }

        User existUser = userMapper.selectByUsername(dto.getUsername());
        if (existUser != null) {
            return Result.error(ResultCode.USERNAME_EXISTS);
        }
        if (dto.getPhone() != null) {
            User phoneUser = userMapper.selectByPhone(dto.getPhone());
            if (phoneUser != null) {
                return Result.error(ResultCode.PHONE_EXISTS);
            }
        }
        if (dto.getEmail() != null) {
            User emailUser = userMapper.selectByEmail(dto.getEmail());
            if (emailUser != null) {
                return Result.error(ResultCode.EMAIL_EXISTS);
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        user.setRealVerified(0);
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(3L);
        userRoleMapper.insert(userRole);

        List<String> permissions = loadUserPermissions(user.getId());
        redisTemplate.opsForValue().set(RedisConstant.PERMISSIONS_PREFIX + user.getId(), permissions,
                RedisConstant.TOKEN_TTL, TimeUnit.SECONDS);

        return generateTokenPair(user);
    }

    @Override
    public Result<TokenVO> login(LoginDTO dto) {
        String ip = IpUtil.getIpAddr();

        String lockKey = AccountLockConstant.LOGIN_FAIL_PREFIX + dto.getUsername();
        Object lockObj = redisTemplate.opsForValue().get(lockKey);
        if (lockObj != null) {
            long failCount = Long.parseLong(lockObj.toString());
            if (failCount >= AccountLockConstant.MAX_LOGIN_FAIL) {
                Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
                long remainMinutes = ttl != null && ttl > 0 ? ttl : AccountLockConstant.LOCK_DURATION / 60;
                logService.recordSecurityLog(buildSecurityLog(null, dto.getUsername(), SecurityEventType.RATE_LIMIT.getCode(), ip, "账号已锁定"));
                return Result.error(423, "账号已锁定，请" + remainMinutes + "分钟后重试");
            }
        }

        String rateLimitKey = RedisConstant.RATE_LIMIT_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(rateLimitKey, RedisConstant.RATE_LIMIT_TTL, TimeUnit.SECONDS);
        }
        if (count != null && count > SecurityConstant.LOGIN_RATE_LIMIT) {
            logService.recordSecurityLog(buildSecurityLog(null, dto.getUsername(), SecurityEventType.RATE_LIMIT.getCode(), ip, "登录频率过高"));
            return Result.error(ResultCode.RATE_LIMIT_EXCEEDED);
        }

        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            Long failCount = redisTemplate.opsForValue().increment(lockKey);
            if (failCount != null && failCount == 1) {
                redisTemplate.expire(lockKey, AccountLockConstant.LOCK_DURATION, TimeUnit.SECONDS);
            }
            long remaining = AccountLockConstant.MAX_LOGIN_FAIL - (failCount != null ? failCount : 1);
            String msg = remaining <= 0 ? "账号已锁定，请30分钟后重试" : "用户名或密码错误，剩余" + remaining + "次尝试机会";
            logService.recordSecurityLog(buildSecurityLog(null, dto.getUsername(), SecurityEventType.LOGIN_FAIL.getCode(), ip, msg));
            return Result.error(ResultCode.LOGIN_FAIL.getCode(), msg);
        }
        if (user.getStatus() == 0) {
            return Result.error(ResultCode.ACCOUNT_DISABLED);
        }

        redisTemplate.delete(lockKey);
        redisTemplate.delete(rateLimitKey);

        List<String> permissions = loadUserPermissions(user.getId());
        redisTemplate.opsForValue().set(RedisConstant.PERMISSIONS_PREFIX + user.getId(), permissions,
                RedisConstant.TOKEN_TTL, TimeUnit.SECONDS);

        return generateTokenPair(user);
    }

    @Override
    public Result<Void> logout(Long userId) {
        String tokenKey = RedisConstant.TOKEN_PREFIX + userId;
        String refreshTokenKey = RedisConstant.REFRESH_PREFIX + userId;
        Object accessToken = redisTemplate.opsForValue().get(tokenKey);
        if (accessToken != null) {
            String blacklistKey = RedisConstant.BLACKLIST_PREFIX + accessToken.toString();
            redisTemplate.opsForValue().set(blacklistKey, "1", RedisConstant.TOKEN_TTL, TimeUnit.SECONDS);
        }
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(refreshTokenKey);
        redisTemplate.delete(RedisConstant.PERMISSIONS_PREFIX + userId);
        return Result.success();
    }

    @Override
    public Result<TokenVO> refreshToken(RefreshTokenDTO dto) {
        if (!jwtUtil.validateToken(dto.getRefreshToken()) || !jwtUtil.isRefreshToken(dto.getRefreshToken())) {
            return Result.error(ResultCode.TOKEN_INVALID);
        }
        Long userId = jwtUtil.getUserIdFromToken(dto.getRefreshToken());
        String refreshTokenKey = RedisConstant.REFRESH_PREFIX + userId;
        Object storedToken = redisTemplate.opsForValue().get(refreshTokenKey);
        if (storedToken == null || !storedToken.equals(dto.getRefreshToken())) {
            return Result.error(ResultCode.TOKEN_INVALID);
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            return Result.error(ResultCode.ACCOUNT_DISABLED);
        }

        List<String> permissions = loadUserPermissions(user.getId());
        redisTemplate.opsForValue().set(RedisConstant.PERMISSIONS_PREFIX + user.getId(), permissions,
                RedisConstant.TOKEN_TTL, TimeUnit.SECONDS);

        return generateTokenPair(user);
    }

    private List<String> loadUserPermissions(Long userId) {
        List<String> authorities = new ArrayList<>();
        List<Role> roles = roleMapper.selectByUserId(userId);
        for (Role role : roles) {
            authorities.add(role.getRoleCode());
        }
        List<String> permissionCodes = permissionMapper.selectPermissionCodesByUserId(userId);
        authorities.addAll(permissionCodes);
        return authorities;
    }

    private SecurityLog buildSecurityLog(Long userId, String username, String eventType, String ip, String detail) {
        SecurityLog securityLog = new SecurityLog();
        securityLog.setUserId(userId);
        securityLog.setUsername(username);
        securityLog.setEventType(eventType);
        securityLog.setIp(ip);
        securityLog.setDetail(detail);
        return securityLog;
    }

    private Result<TokenVO> generateTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        redisTemplate.opsForValue().set(RedisConstant.TOKEN_PREFIX + user.getId(), accessToken,
                RedisConstant.TOKEN_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(RedisConstant.REFRESH_PREFIX + user.getId(), refreshToken,
                RedisConstant.REFRESH_TTL, TimeUnit.SECONDS);

        TokenVO tokenVO = new TokenVO();
        tokenVO.setAccessToken(accessToken);
        tokenVO.setRefreshToken(refreshToken);
        tokenVO.setExpiresIn(jwtUtil.getAccessTokenExpiration() / 1000);
        return Result.success(tokenVO);
    }
}
