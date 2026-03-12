package com.kite.app.controller;

import com.kite.auth.annotation.AllowAnonymous;
import com.kite.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/api")
public class HealthController {
    
    @AllowAnonymous
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "scaffold-project");
        data.put("status", "ok");
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }
}
