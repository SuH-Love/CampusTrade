package com.campustrade.aspect;

import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.constant.RedisConstant;
import com.campustrade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Pointcut("@annotation(com.campustrade.aspect.RateLimit)")
    public void rateLimitPointcut() {}

    @Around("rateLimitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String ip = IpUtil.getIpAddr();
        String rateLimitKey = RedisConstant.RATE_LIMIT_PREFIX + ip;

        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(rateLimitKey, RedisConstant.RATE_LIMIT_TTL, TimeUnit.SECONDS);
        }
        if (count != null && count > 60) {
            log.warn("接口限流: ip={}", ip);
            return Result.error(ResultCode.RATE_LIMIT_EXCEEDED);
        }

        return joinPoint.proceed();
    }
}