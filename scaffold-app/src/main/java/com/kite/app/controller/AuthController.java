package com.kite.app.controller;

import com.kite.auth.annotation.AllowAnonymous;
import com.kite.auth.model.LoginUser;
import com.kite.auth.service.AuthenticationService;
import com.kite.auth.service.SessionService;
import com.kite.auth.util.JwtUtils;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.Result;
import com.kite.common.response.ResultCode;
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
    
    private final AuthenticationService authenticationService;
    private final JwtUtils jwtUtils;
    private final SessionService sessionService;
    
    @AllowAnonymous
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // 加载用户
        LoginUser loginUser = authenticationService.loadUserByUsername(request.getUsername());
        if (loginUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 临时实现：密码固定为 admin123
        if (!"admin123".equals(request.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
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
        private String username;
        private String password;
    }
}
