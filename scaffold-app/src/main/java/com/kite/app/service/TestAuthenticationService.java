package com.kite.app.service;

import com.kite.auth.model.LoginUser;
import com.kite.auth.service.AuthenticationService;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 认证服务实现（临时测试用）
 */
@Service
public class TestAuthenticationService implements AuthenticationService {
    
    @Override
    public LoginUser loadUserByUsername(String username) {
        // 临时实现：只支持 admin 用户
        if ("admin".equals(username)) {
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(1L);
            loginUser.setUsername("admin");
            loginUser.setNickname("管理员");
            loginUser.setRoles(Arrays.asList("admin"));
            loginUser.setPermissions(Arrays.asList("*:*:*"));
            return loginUser;
        }
        return null;
    }
    
    @Override
    public LoginUser loadUserById(Long userId) {
        // 临时实现：只支持 userId=1
        if (userId != null && userId == 1L) {
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(1L);
            loginUser.setUsername("admin");
            loginUser.setNickname("管理员");
            loginUser.setRoles(Arrays.asList("admin"));
            loginUser.setPermissions(Arrays.asList("*:*:*"));
            return loginUser;
        }
        return null;
    }
}
