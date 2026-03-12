package com.kite.auth.service;

import cn.hutool.core.util.StrUtil;
import com.kite.auth.config.AuthProperties;
import com.kite.auth.model.LoginUser;
import com.kite.common.constant.CommonConstant;
import com.kite.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Session 管理服务
 */
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;
    
    /**
     * 保存 Session
     */
    public void saveSession(LoginUser loginUser) {
        if (!authProperties.getSession().getEnabled()) {
            return;
        }
        
        String key = getSessionKey(loginUser.getUserId());
        String value = JsonUtils.toJson(loginUser);
        
        redisTemplate.opsForValue().set(
                key,
                value,
                authProperties.getSession().getTimeout(),
                TimeUnit.SECONDS
        );
    }
    
    /**
     * 获取 Session
     */
    public LoginUser getSession(Long userId) {
        if (!authProperties.getSession().getEnabled()) {
            return null;
        }
        
        String key = getSessionKey(userId);
        String value = redisTemplate.opsForValue().get(key);
        
        if (StrUtil.isBlank(value)) {
            return null;
        }
        
        return JsonUtils.parseObject(value, LoginUser.class);
    }
    
    /**
     * 刷新 Session
     */
    public void refreshSession(Long userId) {
        if (!authProperties.getSession().getEnabled()) {
            return;
        }
        
        String key = getSessionKey(userId);
        redisTemplate.expire(key, authProperties.getSession().getTimeout(), TimeUnit.SECONDS);
    }
    
    /**
     * 删除 Session
     */
    public void removeSession(Long userId) {
        if (!authProperties.getSession().getEnabled()) {
            return;
        }
        
        String key = getSessionKey(userId);
        redisTemplate.delete(key);
    }
    
    /**
     * 获取 Session Key
     */
    private String getSessionKey(Long userId) {
        return CommonConstant.LOGIN_USER_KEY + userId;
    }
}
