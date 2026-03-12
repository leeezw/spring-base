package com.kite.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@SpringBootApplication(scanBasePackages = "com.kite")
public class ScaffoldApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ScaffoldApplication.class, args);
    }
}
