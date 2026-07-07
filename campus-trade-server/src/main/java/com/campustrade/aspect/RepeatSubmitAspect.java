package com.campustrade.aspect;

import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RepeatSubmitAspect {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Pointcut("@annotation(com.campustrade.aspect.RepeatSubmit)")
    public void repeatSubmitPointcut() {}

    @Around("repeatSubmitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Long userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            userId = (Long) authentication.getPrincipal();
        }

        if (userId == null) {
            return joinPoint.proceed();
        }

        String apiSignature = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        String md5Key = String.valueOf(apiSignature.hashCode());
        String repeatKey = RedisConstant.REPEAT_PREFIX + userId + ":" + md5Key;

        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(repeatKey, "1", RedisConstant.REPEAT_TTL, TimeUnit.SECONDS);
        if (isFirst == null || !isFirst) {
            log.warn("重复提交: userId={}, api={}", userId, apiSignature);
            return Result.error(ResultCode.REPEAT_SUBMIT);
        }

        return joinPoint.proceed();
    }
}