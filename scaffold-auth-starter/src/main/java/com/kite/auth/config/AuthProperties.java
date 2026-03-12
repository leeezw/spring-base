package com.kite.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证配置属性
 */
@Data
@ConfigurationProperties(prefix = "scaffold.auth")
public class AuthProperties {
    
    /**
     * JWT 配置
     */
    private JwtConfig jwt = new JwtConfig();
    
    /**
     * Session 配置
     */
    private SessionConfig session = new SessionConfig();
    
    /**
     * 排除路径
     */
    private String[] excludePaths = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/health",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/error",
            "/",
            "/index.html",
            "/**/*.html",
            "/**/*.css",
            "/**/*.js",
            "/**/*.ico",
            "/**/*.png",
            "/**/*.jpg",
            "/**/*.svg"
    };
    
    @Data
    public static class JwtConfig {
        /**
         * 密钥
         */
        private String secret = "scaffold-project-jwt-secret-key-2026";
        
        /**
         * 过期时间（秒）
         */
        private Long expire = 7200L;
    }
    
    @Data
    public static class SessionConfig {
        /**
         * 是否启用 Session 管理
         */
        private Boolean enabled = true;
        
        /**
         * Session 超时时间（秒）
         */
        private Long timeout = 86400L;
        
        /**
         * 是否单点登录
         */
        private Boolean singleLogin = false;
    }
}
