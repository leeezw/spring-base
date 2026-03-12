package com.kite.permission.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 权限自动配置
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("com.kite.permission")
public class PermissionAutoConfiguration {
}
