package com.kite.cert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务（证书自动续期）。
 */
@Configuration
@EnableScheduling
public class CertSchedulingConfig {
}
