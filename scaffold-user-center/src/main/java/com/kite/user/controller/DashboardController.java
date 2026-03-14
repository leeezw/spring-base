package com.kite.user.controller;

import com.kite.common.response.Result;
import com.kite.user.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据看板控制器
 */
@RestController
@RequestMapping("/api/system/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取概览统计
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(dashboardService.getOverview());
    }

    /**
     * 近N天用户增长趋势
     */
    @GetMapping("/user-trend")
    public Result<Object> getUserTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(dashboardService.getUserTrend(days));
    }

    /**
     * 部门人数分布
     */
    @GetMapping("/dept-distribution")
    public Result<Object> getDeptDistribution() {
        return Result.success(dashboardService.getDeptDistribution());
    }

    /**
     * 角色分布
     */
    @GetMapping("/role-distribution")
    public Result<Object> getRoleDistribution() {
        return Result.success(dashboardService.getRoleDistribution());
    }

    /**
     * 最近登录记录
     */
    @GetMapping("/recent-logins")
    public Result<Object> getRecentLogins(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.getRecentLogins(limit));
    }
}
