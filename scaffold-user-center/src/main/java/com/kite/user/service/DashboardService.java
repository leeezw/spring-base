package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.user.entity.*;
import com.kite.user.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;

/**
 * 数据看板服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final SysPostMapper postMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 概览统计
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // 用户总数
        long userCount = userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0)
        );
        overview.put("userCount", userCount);

        // 启用用户数
        long activeUserCount = userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .eq(SysUser::getStatus, 1)
        );
        overview.put("activeUserCount", activeUserCount);

        // 角色数
        long roleCount = roleMapper.selectCount(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getDeleted, 0)
        );
        overview.put("roleCount", roleCount);

        // 部门数
        long deptCount = deptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0)
        );
        overview.put("deptCount", deptCount);

        // 岗位数
        long postCount = postMapper.selectCount(
            new LambdaQueryWrapper<SysPost>().eq(SysPost::getDeleted, 0)
        );
        overview.put("postCount", postCount);

        // 在线用户数（Redis session keys）
        long onlineCount = 0;
        try {
            Set<String> keys = redisTemplate.keys("session:user:*");
            onlineCount = keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("获取在线用户数失败: {}", e.getMessage());
        }
        overview.put("onlineCount", onlineCount);

        // 今日新增
        long todayNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .ge(SysUser::getCreateTime, LocalDate.now().atStartOfDay())
        );
        overview.put("todayNewUsers", todayNewUsers);

        return overview;
    }

    /**
     * 用户增长趋势
     */
    public List<Map<String, Object>> getUserTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getDeleted, 0)
                    .ge(SysUser::getCreateTime, start)
                    .lt(SysUser::getCreateTime, end)
            );

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.format(fmt));
            point.put("fullDate", date.toString());
            point.put("count", count);
            trend.add(point);
        }
        return trend;
    }

    /**
     * 部门人数分布
     */
    public List<Map<String, Object>> getDeptDistribution() {
        // 查所有部门
        List<SysDept> depts = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .eq(SysDept::getStatus, 1)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysDept dept : depts) {
            long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getDeleted, 0)
                    .eq(SysUser::getDeptId, dept.getId())
            );
            if (count > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("deptName", dept.getDeptName());
                item.put("deptId", dept.getId());
                item.put("count", count);
                result.add(item);
            }
        }

        // 未分配部门的用户
        long noDeptCount = userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .isNull(SysUser::getDeptId)
        );
        if (noDeptCount > 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deptName", "未分配");
            item.put("deptId", null);
            item.put("count", noDeptCount);
            result.add(item);
        }

        // 按人数降序
        result.sort((a, b) -> Long.compare((long) b.get("count"), (long) a.get("count")));
        return result;
    }

    /**
     * 角色用户分布
     */
    public List<Map<String, Object>> getRoleDistribution() {
        List<SysRole> roles = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getDeleted, 0)
                .eq(SysRole::getStatus, 1)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysRole role : roles) {
            long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, role.getId())
            );
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("roleName", role.getRoleName());
            item.put("roleId", role.getId());
            item.put("count", count);
            result.add(item);
        }

        result.sort((a, b) -> Long.compare((long) b.get("count"), (long) a.get("count")));
        return result;
    }

    /**
     * 最近登录记录（从Redis session取）
     */
    public List<Map<String, Object>> getRecentLogins(int limit) {
        List<Map<String, Object>> logins = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Set<String> keys = redisTemplate.keys("session:user:*");
            if (keys != null) {
                for (String key : keys) {
                    try {
                        String val = redisTemplate.opsForValue().get(key);
                        if (val != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> session = objectMapper.readValue(val, Map.class);
                            Map<String, Object> login = new LinkedHashMap<>();
                            login.put("userId", session.get("userId"));
                            login.put("username", session.get("username"));
                            login.put("nickname", session.get("nickname"));
                            login.put("loginTime", session.get("loginTime"));
                            login.put("tenantId", session.get("tenantId"));
                            logins.add(login);
                        }
                    } catch (Exception e) {
                        // skip unparseable sessions
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取登录记录失败: {}", e.getMessage());
        }

        // 按loginTime降序
        logins.sort((a, b) -> {
            Long ta = a.get("loginTime") != null ? ((Number) a.get("loginTime")).longValue() : 0L;
            Long tb = b.get("loginTime") != null ? ((Number) b.get("loginTime")).longValue() : 0L;
            return tb.compareTo(ta);
        });

        return logins.stream().limit(limit).collect(Collectors.toList());
    }
}
