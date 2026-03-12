package com.kite.cache.aspect;

import cn.hutool.core.util.StrUtil;
import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.cache.annotation.RateLimit;
import com.kite.cache.service.RateLimitService;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 限流切面
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RateLimitService rateLimitService;
    
    @Around("@annotation(com.kite.cache.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        
        String key = buildKey(annotation);
        
        boolean allowed = switch (annotation.algorithm()) {
            case SLIDING_WINDOW -> rateLimitService.slidingWindowLimit(
                key, annotation.count(), annotation.period(), annotation.timeUnit()
            );
            case FIXED_WINDOW -> rateLimitService.fixedWindowLimit(
                key, annotation.count(), annotation.period(), annotation.timeUnit()
            );
            case TOKEN_BUCKET -> rateLimitService.tokenBucketLimit(
                key, annotation.count(), annotation.period(), annotation.timeUnit()
            );
        };
        
        if (!allowed) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, annotation.message());
        }
        
        return joinPoint.proceed();
    }
    
    private String buildKey(RateLimit annotation) {
        String key = annotation.key();
        
        if (annotation.limitType() == RateLimit.LimitType.CUSTOM && StrUtil.isNotBlank(key)) {
            return annotation.prefix() + key;
        }
        
        String suffix = switch (annotation.limitType()) {
            case IP -> getIpAddress();
            case USER -> {
                LoginUser loginUser = LoginUserContext.get();
                yield loginUser != null ? String.valueOf(loginUser.getUserId()) : "anonymous";
            }
            case TOKEN -> {
                LoginUser loginUser = LoginUserContext.get();
                yield loginUser != null ? loginUser.getToken() : "no-token";
            }
            case GLOBAL -> "global";
            default -> "unknown";
        };
        
        return annotation.prefix() + suffix;
    }
    
    private String getIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
