package com.kite.auth.service;

import com.kite.auth.model.LoginUser;

/**
 * 认证服务接口
 * 业务层需要实现此接口，提供用户查询功能
 */
public interface AuthenticationService {
    
    /**
     * 根据用户名加载用户信息
     */
    LoginUser loadUserByUsername(String username);
    
    /**
     * 根据用户ID加载用户信息
     */
    LoginUser loadUserById(Long userId);
}
