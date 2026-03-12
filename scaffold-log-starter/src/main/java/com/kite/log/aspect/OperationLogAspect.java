package com.kite.log.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.log.annotation.OperationLog;
import com.kite.log.model.OperationLogRecord;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {
    
    @Around("@annotation(com.kite.log.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        
        OperationLogRecord record = new OperationLogRecord();
        record.setModule(annotation.module());
        record.setOperationType(annotation.type().name());
        record.setDescription(annotation.description());
        record.setMethod(method.getDeclaringClass().getName() + "." + method.getName());
        record.setOperationTime(startTime);
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            record.setRequestUrl(request.getRequestURI());
            record.setIp(getIpAddress(request));
            
            if (annotation.saveRequestData()) {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    String params = JSONUtil.toJsonStr(args);
                    record.setRequestParams(StrUtil.sub(params, 0, 2000));
                }
            }
        }
        
        // 获取用户信息
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser != null) {
            record.setUserId(loginUser.getUserId());
            record.setUsername(loginUser.getUsername());
        }
        
        Object result = null;
        try {
            result = joinPoint.proceed();
            record.setStatus(1);
            
            if (annotation.saveResponseData() && result != null) {
                String response = JSONUtil.toJsonStr(result);
                record.setResponseData(StrUtil.sub(response, 0, 2000));
            }
        } catch (Throwable e) {
            record.setStatus(0);
            record.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 500));
            throw e;
        } finally {
            record.setDuration(System.currentTimeMillis() - startTime);
            saveLog(record);
        }
        
        return result;
    }
    
    private void saveLog(OperationLogRecord record) {
        // 异步保存日志（这里简化为打印，实际应该保存到数据库）
        log.info("操作日志: {}", JSONUtil.toJsonStr(record));
    }
    
    private String getIpAddress(HttpServletRequest request) {
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
