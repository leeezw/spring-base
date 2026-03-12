package com.kite.cache.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 缓存自动配置
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("com.kite.cache")
public class CacheAutoConfiguration {
}
