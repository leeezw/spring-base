package com.kite.app.controller;

import com.kite.cache.annotation.RateLimit;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.permission.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 演示权限、日志、限流功能
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    /**
     * 测试角色权限
     */
    @GetMapping("/admin")
    @RequiresRoles("admin")
    @OperationLog(module = "测试", type = OperationLog.OperationType.QUERY, description = "测试管理员权限")
    public Result<String> testAdmin() {
        return Result.success("你有管理员权限");
    }
    
    /**
     * 测试操作权限
     */
    @GetMapping("/permission")
    @RequiresPermissions("system:user:query")
    @OperationLog(module = "测试", type = OperationLog.OperationType.QUERY, description = "测试操作权限")
    public Result<String> testPermission() {
        return Result.success("你有查询用户权限");
    }
    
    /**
     * 测试限流 - IP限流，每分钟最多5次
     */
    @GetMapping("/rate-limit-ip")
    @RateLimit(limitType = RateLimit.LimitType.IP, count = 5, period = 60)
    public Result<String> testRateLimitByIp() {
        return Result.success("IP限流测试成功");
    }
    
    /**
     * 测试限流 - 用户限流，每分钟最多10次
     */
    @GetMapping("/rate-limit-user")
    @RateLimit(limitType = RateLimit.LimitType.USER, count = 10, period = 60)
    public Result<String> testRateLimitByUser() {
        return Result.success("用户限流测试成功");
    }
    
    /**
     * 测试限流 - 全局限流，每分钟最多100次
     */
    @GetMapping("/rate-limit-global")
    @RateLimit(limitType = RateLimit.LimitType.GLOBAL, count = 100, period = 60)
    public Result<String> testRateLimitGlobal() {
        return Result.success("全局限流测试成功");
    }
    
    /**
     * 测试操作日志
     */
    @GetMapping("/log")
    @OperationLog(
        module = "测试模块",
        type = OperationLog.OperationType.QUERY,
        description = "测试操作日志记录",
        saveRequestData = true,
        saveResponseData = true
    )
    public Result<Map<String, Object>> testLog() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "操作日志测试");
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }
    
    /**
     * 综合测试 - 权限+日志+限流
     */
    @GetMapping("/combined")
    @RequiresRoles("admin")
    @RequiresPermissions("system:test:query")
    @OperationLog(module = "测试", type = OperationLog.OperationType.QUERY, description = "综合功能测试")
    @RateLimit(limitType = RateLimit.LimitType.USER, count = 3, period = 60)
    public Result<String> testCombined() {
        return Result.success("综合测试成功：权限验证通过 + 操作日志已记录 + 限流生效");
    }
}
