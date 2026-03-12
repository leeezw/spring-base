package com.kite.app.controller;

import com.kite.auth.annotation.AllowAnonymous;
import com.kite.auth.model.LoginUser;
import com.kite.auth.service.SessionService;
import com.kite.auth.util.JwtUtils;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.Result;
import com.kite.common.response.ResultCode;
import com.kite.user.service.UserAuthenticationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserAuthenticationService userAuthenticationService;
    private final JwtUtils jwtUtils;
    private final SessionService sessionService;
    
    @AllowAnonymous
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // 验证租户编码（如果未提供，默认使用default）
        String tenantCode = request.getTenantCode();
        if (tenantCode == null || tenantCode.isEmpty()) {
            tenantCode = "default";
        }
        
        // 使用租户编码 + 用户名 + 密码认证
        LoginUser loginUser = userAuthenticationService.authenticate(
            tenantCode, 
            request.getUsername(), 
            request.getPassword()
        );
        
        if (loginUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 生成 Token
        String token = jwtUtils.generateToken(loginUser.getUserId(), loginUser.getUsername());
        loginUser.setToken(token);
        loginUser.setLoginTime(System.currentTimeMillis());
        
        // 保存 Session
        sessionService.saveSession(loginUser);
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", loginUser);
        
        return Result.success(data);
    }
    
    @Data
    public static class LoginRequest {
        private String tenantCode;  // 租户编码
        private String username;
        private String password;
    }
}
