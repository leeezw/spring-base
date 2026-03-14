package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.auth.model.LoginUser;
import com.kite.auth.service.AuthenticationService;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysTenant;
import com.kite.user.entity.SysUser;
import com.kite.user.entity.SysLoginLog;
import com.kite.user.mapper.SysTenantMapper;
import com.kite.user.mapper.SysUserMapper;
import com.kite.user.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户认证服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationService implements AuthenticationService {
    
    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SysLoginLogMapper loginLogMapper;

    @Value("${app.log.login.enabled:true}")
    private boolean loginLogEnabled;
    
    @Override
    public LoginUser loadUserByUsername(String username) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
        );
        
        if (user == null || user.getStatus() != 1) {
            return null;
        }
        
        return buildLoginUser(user);
    }
    
    @Override
    public LoginUser loadUserById(Long userId) {
        // 通过主键查询，不需要租户过滤
        TenantContext.setIgnore(true);
        try {
            SysUser user = userMapper.selectById(userId);
            if (user == null || user.getDeleted() == 1 || user.getStatus() != 1) {
                return null;
            }
            
            return buildLoginUser(user);
        } finally {
            TenantContext.clear();
        }
    }
    
    /**
     * 租户编码 + 用户名 + 密码认证
     */
    public LoginUser authenticate(String tenantCode, String username, String password) {
        // 1. 查询租户
        SysTenant tenant = tenantMapper.selectOne(
            new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantCode, tenantCode)
                .eq(SysTenant::getDeleted, 0)
        );
        
        if (tenant == null || tenant.getStatus() != 1) {
            saveLoginLog(username, 0, "租户不存在或已禁用", null);
            return null;
        }
        
        // 2. 设置租户上下文（临时，用于查询用户）
        Long tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
        
        try {
            // 3. 查询用户（会自动注入 WHERE tenant_id = ?）
            SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, username)
                    .eq(SysUser::getDeleted, 0)
            );
            
            if (user == null) {
                saveLoginLog(username, 0, "用户不存在", tenantId);
                return null;
            }
            
            // 4. 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                saveLoginLog(username, 0, "密码错误", tenantId);
                return null;
            }
            
            // 5. 检查状态
            if (user.getStatus() != 1) {
                saveLoginLog(username, 0, "账号已禁用", tenantId);
                return null;
            }
            
            saveLoginLog(username, 1, "登录成功", tenantId);
            return buildLoginUser(user);
        } finally {
            // 清除临时租户上下文
            TenantContext.clear();
        }
    }
    
    /**
     * 用户名密码认证（已废弃，使用 authenticate(tenantCode, username, password)）
     */
    @Deprecated
    public LoginUser authenticate(String username, String password) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
        );
        
        if (user == null) {
            return null;
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        
        if (user.getStatus() != 1) {
            return null;
        }
        
        return buildLoginUser(user);
    }
    
    private LoginUser buildLoginUser(SysUser user) {
        List<String> roles = userMapper.selectRolesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionsByUserId(user.getId());
        
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setTenantId(user.getTenantId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        
        return loginUser;
    }
    
    /**
     * 预登录：验证用户名密码（跨租户），返回该用户所属的租户列表
     * 用于两阶段登录流程：先验证身份，再选择租户
     */
    public List<Map<String, Object>> preLogin(String username, String password) {
        // 跨租户查询所有匹配用户名的用户
        TenantContext.setIgnore(true);
        try {
            List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, username)
                    .eq(SysUser::getDeleted, 0)
                    .eq(SysUser::getStatus, 1)
            );
            
            List<Map<String, Object>> tenantList = new ArrayList<>();
            
            for (SysUser user : users) {
                // 验证密码
                if (passwordEncoder.matches(password, user.getPassword())) {
                    // 查询该用户所属的租户信息
                    SysTenant tenant = tenantMapper.selectOne(
                        new LambdaQueryWrapper<SysTenant>()
                            .eq(SysTenant::getId, user.getTenantId())
                            .eq(SysTenant::getDeleted, 0)
                            .eq(SysTenant::getStatus, 1)
                    );
                    
                    if (tenant != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("tenantId", tenant.getId());
                        item.put("tenantCode", tenant.getTenantCode());
                        item.put("tenantName", tenant.getTenantName());
                        item.put("userId", user.getId());
                        item.put("nickname", user.getNickname());
                        item.put("avatar", user.getAvatar());
                        tenantList.add(item);
                    }
                }
            }
            
            return tenantList;
        } finally {
            TenantContext.clear();
        }
    }

    private void saveLoginLog(String username, int status, String msg, Long tenantId) {
        if (!loginLogEnabled) return;
        try {
            TenantContext.setIgnore(true);
            SysLoginLog logEntry = new SysLoginLog();
            logEntry.setUsername(username);
            logEntry.setStatus(status);
            logEntry.setMessage(msg);
            logEntry.setTenantId(tenantId != null ? tenantId : 0L);
            logEntry.setLoginTime(LocalDateTime.now());

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("X-Real-IP");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = req.getRemoteAddr();
                if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
                logEntry.setIp(ip);
                logEntry.setUserAgent(req.getHeader("User-Agent"));
            }

            loginLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("保存登录日志失败: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
