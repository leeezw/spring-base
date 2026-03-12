package com.kite.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    
    /**
     * 锁的key，支持SpEL表达式
     */
    String key();
    
    /**
     * 锁的前缀
     */
    String prefix() default "lock:";
    
    /**
     * 等待时间
     */
    long waitTime() default 3;
    
    /**
     * 持有时间
     */
    long leaseTime() default 10;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
