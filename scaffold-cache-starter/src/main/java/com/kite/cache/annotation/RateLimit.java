package com.kite.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流key，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 限流前缀
     */
    String prefix() default "rate_limit:";
    
    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.IP;
    
    /**
     * 时间窗口内允许的请求数
     */
    int count() default 100;
    
    /**
     * 时间窗口大小
     */
    int period() default 60;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 限流算法
     */
    Algorithm algorithm() default Algorithm.SLIDING_WINDOW;
    
    /**
     * 限流提示信息
     */
    String message() default "访问过于频繁，请稍后再试";
    
    enum LimitType {
        /**
         * 根据IP限流
         */
        IP,
        
        /**
         * 根据用户ID限流
         */
        USER,
        
        /**
         * 根据Token限流
         */
        TOKEN,
        
        /**
         * 全局限流
         */
        GLOBAL,
        
        /**
         * 自定义key限流
         */
        CUSTOM
    }
    
    enum Algorithm {
        /**
         * 滑动窗口
         */
        SLIDING_WINDOW,
        
        /**
         * 固定窗口
         */
        FIXED_WINDOW,
        
        /**
         * 令牌桶
         */
        TOKEN_BUCKET
    }
}
