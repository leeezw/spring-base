package com.kite.auth.service;

import com.kite.common.constant.CommonConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 将 Token 加入黑名单
     * @param token Token
     * @param expireSeconds 过期时间（秒）
     */
    public void addToBlacklist(String token, Long expireSeconds) {
        String key = getBlacklistKey(token);
        redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = getBlacklistKey(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * 从黑名单中移除 Token
     */
    public void removeFromBlacklist(String token) {
        String key = getBlacklistKey(token);
        redisTemplate.delete(key);
    }
    
    /**
     * 获取黑名单 Key
     */
    private String getBlacklistKey(String token) {
        return CommonConstant.TOKEN_BLACKLIST_KEY + token;
    }
}
