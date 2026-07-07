package com.campustrade.aspect;

import com.campustrade.entity.OperationLog;
import com.campustrade.service.LogService;
import com.campustrade.util.IpUtil;
import com.campustrade.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private LogService logService;

    @Pointcut("execution(* com.campustrade.controller..*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Integer status = 1;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = 0;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            recordLog(joinPoint, duration, status, errorMsg);
        }
    }

    private void recordLog(ProceedingJoinPoint joinPoint, long duration, Integer status, String errorMsg) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;

            HttpServletRequest request = attributes.getRequest();
            Long userId = null;
            String username = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Long) {
                userId = (Long) authentication.getPrincipal();
                username = authentication.getDetails() != null ? authentication.getDetails().toString() : null;
            }

            OperationLog opLog = new OperationLog();
            opLog.setUserId(userId);
            opLog.setUsername(username);
            opLog.setModule(joinPoint.getTarget().getClass().getSimpleName().replace("Controller", ""));
            opLog.setOperation(joinPoint.getSignature().getName());
            opLog.setMethod(request.getMethod());
            opLog.setRequestUrl(request.getRequestURI());
            opLog.setIp(IpUtil.getIpAddr());
            opLog.setDuration(duration);
            opLog.setStatus(status);
            opLog.setErrorMsg(errorMsg);
            opLog.setTraceId(TraceIdUtil.get());
            opLog.setCreateTime(LocalDateTime.now());

            logService.recordOperationLog(opLog);
        } catch (Exception e) {
            log.warn("记录操作日志失败", e);
        }
    }
}