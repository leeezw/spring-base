package com.kite.auth.config;

import com.kite.auth.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 认证自动配置
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
@ComponentScan("com.kite.auth")
@RequiredArgsConstructor
public class AuthAutoConfiguration {
    
    private final AuthenticationFilter authenticationFilter;
    
    /**
     * 注册认证过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
