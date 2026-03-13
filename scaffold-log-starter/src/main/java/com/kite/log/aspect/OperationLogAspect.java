package com.kite.log.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.log.annotation.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 操作日志切面 — 支持开关控制
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Value("${app.log.operation.enabled:true}")
    private boolean enabled;

    @Value("${app.log.operation.save-response:false}")
    private boolean globalSaveResponse;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Around("@annotation(com.kite.log.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!enabled) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        // 收集请求上下文
        String module = annotation.module();
        String operationType = annotation.type().name();
        String description = annotation.description();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String requestUrl = null;
        String requestMethod = null;
        String ip = null;
        String userAgent = null;
        String requestParams = null;

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            requestUrl = request.getRequestURI();
            requestMethod = request.getMethod();
            ip = getIpAddress(request);
            userAgent = StrUtil.sub(request.getHeader("User-Agent"), 0, 500);

            if (annotation.saveRequestData()) {
                try {
                    Object[] args = joinPoint.getArgs();
                    if (args != null && args.length > 0) {
                        requestParams = StrUtil.sub(JSONUtil.toJsonStr(args), 0, 2000);
                    }
                } catch (Exception e) {
                    requestParams = "[序列化失败]";
                }
            }
        }

        Long userId = null;
        String username = null;
        Long tenantId = null;
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser != null) {
            userId = loginUser.getUserId();
            username = loginUser.getUsername();
            tenantId = loginUser.getTenantId();
        }

        Object result = null;
        int status = 1;
        String errorMsg = null;
        String responseData = null;

        try {
            result = joinPoint.proceed();
            if ((annotation.saveResponseData() || globalSaveResponse) && result != null) {
                try {
                    responseData = StrUtil.sub(JSONUtil.toJsonStr(result), 0, 2000);
                } catch (Exception ignored) {}
            }
        } catch (Throwable e) {
            status = 0;
            errorMsg = StrUtil.sub(e.getMessage(), 0, 500);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // 异步入库
            saveLogAsync(module, operationType, description, methodName, requestUrl, requestMethod,
                    requestParams, responseData, status, errorMsg, ip, userAgent, duration,
                    userId, username, tenantId);
        }

        return result;
    }

    private void saveLogAsync(String module, String operationType, String description,
                              String method, String requestUrl, String requestMethod,
                              String requestParams, String responseData, int status, String errorMsg,
                              String ip, String userAgent, long duration,
                              Long userId, String username, Long tenantId) {
        try {
            if (jdbcTemplate == null) {
                log.info("操作日志(无DB): module={} type={} desc={} user={} ip={} {}ms",
                        module, operationType, description, username, ip, duration);
                return;
            }
            jdbcTemplate.update(
                "INSERT INTO sys_operation_log (module, operation_type, description, method, request_url, request_method, " +
                "request_params, response_data, status, error_msg, ip, user_agent, duration, user_id, username, tenant_id, create_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                module, operationType, description, method, requestUrl, requestMethod,
                requestParams, responseData, status, errorMsg, ip, userAgent, duration,
                userId, username, tenantId != null ? tenantId : 1, Timestamp.valueOf(LocalDateTime.now())
            );
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
