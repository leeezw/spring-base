package com.kite.log.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 日志自动配置
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("com.kite.log")
public class LogAutoConfiguration {
}
