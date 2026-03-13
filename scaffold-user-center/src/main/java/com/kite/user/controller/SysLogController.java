package com.kite.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.common.response.Result;
import com.kite.auth.service.SessionService;
import com.kite.auth.model.LoginUser;
import com.kite.user.entity.SysLoginLog;
import com.kite.user.entity.SysOperationLog;
import com.kite.user.mapper.SysLoginLogMapper;
import com.kite.user.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.kite.common.util.JsonUtils;

import java.util.*;

@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final SessionService sessionService;

    /**
     * 操作日志分页
     */
    @GetMapping("/operation")
    public Result<?> operationLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String operationType) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(module), SysOperationLog::getModule, module)
               .like(StringUtils.hasText(username), SysOperationLog::getUsername, username)
               .eq(status != null, SysOperationLog::getStatus, status)
               .eq(StringUtils.hasText(operationType), SysOperationLog::getOperationType, operationType)
               .orderByDesc(SysOperationLog::getCreateTime);
        return Result.success(operationLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    /**
     * 登录日志分页
     */
    @GetMapping("/login")
    public Result<?> loginLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysLoginLog::getUsername, username)
               .eq(status != null, SysLoginLog::getStatus, status)
               .orderByDesc(SysLoginLog::getLoginTime);
        return Result.success(loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    /**
     * 清空操作日志
     */
    @DeleteMapping("/operation/clean")
    public Result<Void> cleanOperationLogs() {
        operationLogMapper.delete(new LambdaQueryWrapper<>());
        return Result.success();
    }

    /**
     * 清空登录日志
     */
    @DeleteMapping("/login/clean")
    public Result<Void> cleanLoginLogs() {
        loginLogMapper.delete(new LambdaQueryWrapper<>());
        return Result.success();
    }

    /**
     * 在线用户列表
     */
    @GetMapping("/online")
    public Result<List<Map<String, Object>>> onlineUsers() {
        Set<String> keys = redisTemplate.keys("login:user:*");
        List<Map<String, Object>> list = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) continue;
                try {
                    LoginUser user = JsonUtils.parseObject(json, LoginUser.class);
                    if (user != null) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("userId", user.getUserId());
                        item.put("username", user.getUsername());
                        item.put("nickname", user.getNickname());
                        item.put("tenantId", user.getTenantId());
                        Long ttl = redisTemplate.getExpire(key);
                        item.put("ttl", ttl);
                        list.add(item);
                    }
                } catch (Exception ignored) {}
            }
        }
        list.sort((a, b) -> Long.compare((Long)b.get("userId"), (Long)a.get("userId")));
        return Result.success(list);
    }

    /**
     * 强制下线
     */
    @DeleteMapping("/online/{userId}")
    public Result<Void> forceLogout(@PathVariable Long userId) {
        sessionService.removeSession(userId);
        return Result.success();
    }
}
