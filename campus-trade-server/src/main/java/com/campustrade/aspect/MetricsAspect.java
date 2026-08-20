package com.campustrade.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

@Aspect
@Component
public class MetricsAspect {

    private static final String MODULE_TAG = "module";
    private static final String METHOD_TAG = "method";

    @Autowired
    private MeterRegistry meterRegistry;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}
    
    @Around("controllerPointcut()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String module = resolveModule(joinPoint, signature);
        String method = signature.getName();

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = true;
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            sample.stop(Timer.builder("http.server.requests.campus")
                    .description("Controller request latency")
                    .tag(MODULE_TAG, module)
                    .tag(METHOD_TAG, method)
                    .tag("status", success ? "success" : "error")
                    .register(meterRegistry));
            Counter.builder("http.server.requests.campus.total")
                    .tag(MODULE_TAG, module)
                    .tag(METHOD_TAG, method)
                    .tag("status", success ? "success" : "error")
                    .register(meterRegistry)
                    .increment();
        }
    }

    private String resolveModule(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        Object target = joinPoint.getTarget();
        RequestMapping classMapping = target.getClass().getAnnotation(RequestMapping.class);
        String module = null;
        if (classMapping != null && classMapping.value().length > 0) {
            module = extractModule(classMapping.value()[0]);
        }
        if (module == null || module.isEmpty() || "unknown".equals(module)) {
            RequestMapping methodMapping = signature.getMethod().getAnnotation(RequestMapping.class);
            if (methodMapping != null && methodMapping.value().length > 0) {
                module = extractModule(methodMapping.value()[0]);
            }
        }
        if (module != null && !"unknown".equals(module)) {
            return module;
        }
        String className = target.getClass().getSimpleName();
        if (className.endsWith("Controller")) {
            return className.substring(0, className.length() - "Controller".length()).toLowerCase();
        }
        return className;
    }

    private String extractModule(String path) {
        if (path == null || path.isEmpty()) return "unknown";
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slash = trimmed.indexOf('/');
        String first = slash > 0 ? trimmed.substring(0, slash) : trimmed;
        if ("api".equals(first)) {
            int firstSlash = trimmed.indexOf('/');
            int second = trimmed.indexOf('/', firstSlash + 1);
            if (second > 0) {
                return trimmed.substring(firstSlash + 1, second);
            }
            return trimmed.substring(firstSlash + 1);
        }
        return first;
    }
}