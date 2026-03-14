package com.kite.app.controller;

import com.kite.auth.annotation.AllowAnonymous;
import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.auth.service.SessionService;
import com.kite.auth.util.JwtUtils;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.Result;
import com.kite.common.response.ResultCode;
import com.kite.user.entity.SysUser;
import com.kite.user.service.UserAuthenticationService;
import com.kite.user.service.SysUserService;
import com.kite.user.service.SysPostService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    private final SysUserService sysUserService;
    private final SysPostService sysPostService;
    
    /**
     * 预登录：验证用户名密码，返回该用户所属的租户列表
     * 用于两阶段登录流程
     */
    @AllowAnonymous
    @PostMapping("/pre-login")
    public Result<List<Map<String, Object>>> preLogin(@RequestBody PreLoginRequest request) {
        List<Map<String, Object>> tenants = userAuthenticationService.preLogin(
            request.getUsername(),
            request.getPassword()
        );
        
        if (tenants == null || tenants.isEmpty()) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户名或密码错误");
        }
        
        return Result.success(tenants);
    }
    
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
    
    /**
     * 获取当前用户详细信息（个人中心用）
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) throw new BusinessException("未登录");
        
        SysUser user = sysUserService.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(null);
        
        // 获取session里的权限信息
        LoginUser loginUser = LoginUserContext.get();
        
        // 查岗位ID
        List<Long> postIds = sysPostService.getUserPostIds(userId);
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("user", user);
        profile.put("roles", loginUser != null ? loginUser.getRoles() : List.of());
        profile.put("permissions", loginUser != null ? loginUser.getPermissions() : List.of());
        profile.put("postIds", postIds);
        
        return Result.success(profile);
    }

    /**
     * 修改个人信息（昵称、邮箱、手机号）
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) throw new BusinessException("未登录");
        
        SysUser update = new SysUser();
        update.setId(userId);
        update.setNickname(request.getNickname());
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());
        update.setAvatar(request.getAvatar());
        sysUserService.updateById(update);
        
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) throw new BusinessException("未登录");
        
        sysUserService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = LoginUserContext.getUserId();
        if (userId != null) {
            sessionService.removeSession(userId);
        }
        return Result.success();
    }
    
    @Data
    public static class PreLoginRequest {
        private String username;
        private String password;
    }
    
    @Data
    public static class LoginRequest {
        private String tenantCode;
        private String username;
        private String password;
    }

    @Data
    public static class UpdateProfileRequest {
        private String nickname;
        private String email;
        private String phone;
        private String avatar;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
