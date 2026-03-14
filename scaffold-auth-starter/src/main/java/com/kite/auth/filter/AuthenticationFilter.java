package com.kite.auth.filter;

import cn.hutool.core.util.StrUtil;
import com.kite.auth.annotation.AllowAnonymous;
import com.kite.auth.config.AuthProperties;
import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.auth.service.AuthenticationService;
import com.kite.auth.service.SessionService;
import com.kite.auth.service.TokenBlacklistService;
import com.kite.auth.util.JwtUtils;
import com.kite.common.response.Result;
import com.kite.common.response.ResultCode;
import com.kite.common.util.JsonUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtils jwtUtils;
    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthProperties authProperties;
    private final RequestMappingHandlerMapping handlerMapping;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        
        // 检查是否在排除路径中
        if (isExcludePath(uri)) {
            chain.doFilter(request, response);
            return;
        }
        
        // 检查是否有 @AllowAnonymous 注解
        if (hasAllowAnonymous(request)) {
            chain.doFilter(request, response);
            return;
        }
        
        // 获取 Token
        String token = getToken(request);
        if (StrUtil.isBlank(token)) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return;
        }
        
        // 检查 Token 是否在黑名单中
        if (tokenBlacklistService.isBlacklisted(token)) {
            writeErrorResponse(response, ResultCode.TOKEN_BLACKLIST);
            return;
        }
        
        // 解析 Token
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            writeErrorResponse(response, ResultCode.TOKEN_INVALID);
            return;
        }
        
        // 检查 Token 是否过期
        if (jwtUtils.isTokenExpired(token)) {
            writeErrorResponse(response, ResultCode.TOKEN_EXPIRED);
            return;
        }
        
        // 加载用户信息
        LoginUser loginUser = authenticationService.loadUserById(userId);
        if (loginUser == null) {
            writeErrorResponse(response, ResultCode.USER_NOT_FOUND);
            return;
        }
        
        // 设置 Token
        loginUser.setToken(token);
        
        // 设置到上下文
        LoginUserContext.set(loginUser);
        
        // 设置租户上下文
        if (loginUser.getTenantId() != null) {
            try {
                Class<?> tenantContextClass = Class.forName("com.kite.mybatis.context.TenantContext");
                Method setTenantIdMethod = tenantContextClass.getMethod("setTenantId", Long.class);
                setTenantIdMethod.invoke(null, loginUser.getTenantId());
            } catch (Exception e) {
                log.warn("设置租户上下文失败: {}", e.getMessage());
            }
        }
        
        // 刷新 Session
        sessionService.refreshSession(userId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
            // 清除租户上下文
            try {
                Class<?> tenantContextClass = Class.forName("com.kite.mybatis.context.TenantContext");
                Method clearMethod = tenantContextClass.getMethod("clear");
                clearMethod.invoke(null);
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    /**
     * 获取 Token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        // 支持URL参数传递token(用于文件下载/导出)
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        }
        return token;
    }
    
    /**
     * 检查是否在排除路径中
     */
    private boolean isExcludePath(String uri) {
        // 静态资源直接放行
        if (!uri.startsWith("/api/")) {
            return true;
        }
        
        for (String pattern : authProperties.getExcludePaths()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否有 @AllowAnonymous 注解
     */
    private boolean hasAllowAnonymous(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) chain.getHandler();
                
                // 检查方法上的注解
                if (handlerMethod.hasMethodAnnotation(AllowAnonymous.class)) {
                    return true;
                }
                
                // 检查类上的注解
                if (handlerMethod.getBeanType().isAnnotationPresent(AllowAnonymous.class)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("检查 @AllowAnonymous 注解失败: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(Result.fail(resultCode)));
    }
}
