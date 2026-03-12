package com.kite.auth.util;

import cn.hutool.core.util.StrUtil;
import com.kite.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {
    
    private final AuthProperties authProperties;
    
    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }
    
    /**
     * 生成 Token（带额外信息）
     */
    public String generateToken(Long userId, String username, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        long expireTime = now + authProperties.getJwt().getExpire() * 1000;
        
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date(now))
                .expiration(new Date(expireTime))
                .signWith(getSecretKey());
        
        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }
        
        return builder.compact();
    }
    
    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return Long.parseLong(claims.getSubject());
    }
    
    /**
     * 从 Token 中获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("username", String.class);
    }
    
    /**
     * 验证 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
    }
    
    /**
     * 获取密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
