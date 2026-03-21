package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysDept;
import com.kite.user.entity.SysRole;
import com.kite.user.entity.SysUser;
import com.kite.user.entity.SysUserRole;
import com.kite.user.entity.SysPost;
import com.kite.user.entity.SysUserPost;
import com.kite.user.mapper.SysDeptMapper;
import com.kite.user.mapper.SysRoleMapper;
import com.kite.user.mapper.SysUserMapper;
import com.kite.user.mapper.SysUserRoleMapper;
import com.kite.user.mapper.SysPostMapper;
import com.kite.user.mapper.SysUserPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysPostMapper postMapper;
    private final DataScopeService dataScopeService;
    
    /**
     * 分页查询用户（含部门名、角色列表）
     */
    public PageResult<SysUser> pageUsers(int pageNum, int pageSize, String username, String nickname, Integer status) {
        return pageUsers(pageNum, pageSize, username, nickname, status, null);
    }
    
    /**
     * 分页查询用户（含部门筛选）
     */
    public PageResult<SysUser> pageUsers(int pageNum, int pageSize, String username, String nickname, Integer status, Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
               .like(StringUtils.hasText(nickname), SysUser::getNickname, nickname)
               .eq(status != null, SysUser::getStatus, status)
               .eq(deptId != null, SysUser::getDeptId, deptId)
               .orderByDesc(SysUser::getCreateTime);
        
        // 数据权限过滤
        DataScopeService.DataScope scope = dataScopeService.getDataScope();
        if (!scope.isAll()) {
            if (scope.isSelfOnly()) {
                // 仅本人数据
                wrapper.eq(SysUser::getId, scope.getUserId());
            } else if (scope.getDeptIds() != null && !scope.getDeptIds().isEmpty()) {
                // 按部门过滤（如果URL参数也指定了deptId，取交集）
                if (deptId != null) {
                    if (!scope.getDeptIds().contains(deptId)) {
                        // 请求的部门不在权限范围内，返回空
                        return new PageResult<>(Collections.emptyList(), 0L, (long) pageNum, (long) pageSize);
                    }
                } else {
                    wrapper.in(SysUser::getDeptId, scope.getDeptIds());
                }
            } else {
                // 无部门且非全部，只能看自己
                wrapper.eq(SysUser::getId, scope.getUserId());
            }
        }
        
        Page<SysUser> page = page(new Page<>(pageNum, pageSize), wrapper);
        List<SysUser> records = page.getRecords();
        
        if (!records.isEmpty()) {
            // 收集所有用户ID和部门ID
            List<Long> userIds = records.stream().map(SysUser::getId).collect(Collectors.toList());
            Set<Long> deptIds = records.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
            
            // 批量查部门
            Map<Long, String> deptNameMap = new HashMap<>();
            if (!deptIds.isEmpty()) {
                List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
                depts.forEach(d -> deptNameMap.put(d.getId(), d.getDeptName()));
            }
            
            // 批量查用户-角色关联
            List<SysUserRole> allUserRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
            Map<Long, List<Long>> userRoleMap = allUserRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                    Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
            
            // 批量查角色信息
            Set<Long> allRoleIds = allUserRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, SysRole> roleInfoMap = new HashMap<>();
            if (!allRoleIds.isEmpty()) {
                List<SysRole> roles = roleMapper.selectBatchIds(allRoleIds);
                roles.forEach(r -> roleInfoMap.put(r.getId(), r));
            }
            
            // 填充到每个用户
            records.forEach(user -> {
                user.setPassword(null);
                user.setDeptName(deptNameMap.get(user.getDeptId()));
                
                List<Long> roleIds = userRoleMap.getOrDefault(user.getId(), Collections.emptyList());
                List<SysUser.RoleInfo> roleInfos = roleIds.stream()
                    .map(roleInfoMap::get)
                    .filter(Objects::nonNull)
                    .map(r -> new SysUser.RoleInfo(r.getId(), r.getRoleCode(), r.getRoleName()))
                    .collect(Collectors.toList());
                user.setRoles(roleInfos);
            });
            
            // 批量查用户-岗位关联
            List<SysUserPost> allUserPosts = userPostMapper.selectList(
                new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, userIds));
            Map<Long, List<Long>> userPostMap = allUserPosts.stream()
                .collect(Collectors.groupingBy(SysUserPost::getUserId,
                    Collectors.mapping(SysUserPost::getPostId, Collectors.toList())));
            
            // 批量查岗位信息
            Set<Long> allPostIds = allUserPosts.stream().map(SysUserPost::getPostId).collect(Collectors.toSet());
            Map<Long, SysPost> postInfoMap = new HashMap<>();
            if (!allPostIds.isEmpty()) {
                List<SysPost> posts = postMapper.selectBatchIds(allPostIds);
                posts.forEach(p -> postInfoMap.put(p.getId(), p));
            }
            
            // 填充岗位
            records.forEach(user -> {
                List<Long> postIds = userPostMap.getOrDefault(user.getId(), Collections.emptyList());
                List<SysUser.PostInfo> postInfos = postIds.stream()
                    .map(postInfoMap::get)
                    .filter(Objects::nonNull)
                    .map(p -> new SysUser.PostInfo(p.getId(), p.getPostCode(), p.getPostName()))
                    .collect(Collectors.toList());
                user.setPosts(postInfos);
            });
        }
        
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
    
    /**
     * 新增用户
     */
    public void addUser(SysUser user) {
        String username = normalizeUsername(user.getUsername());
        user.setUsername(username);
        Long tenantId = resolveTenantId(user.getTenantId());
        user.setTenantId(tenantId);
        ensureUsernameNotExists(username, tenantId, null);
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        save(user);
    }
    
    /**
     * 更新用户
     */
    public void updateUser(SysUser user) {
        SysUser existUser = getById(user.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        
        String newUsername = user.getUsername();
        if (!StringUtils.hasText(newUsername)) {
            newUsername = existUser.getUsername();
        }
        newUsername = normalizeUsername(newUsername);
        user.setUsername(newUsername);
        Long tenantId = existUser.getTenantId();
        user.setTenantId(tenantId);
        
        // 如果修改了用户名，检查是否重复
        if (!existUser.getUsername().equals(newUsername)) {
            ensureUsernameNotExists(newUsername, tenantId, user.getId());
        }
        
        // 不更新密码
        user.setPassword(null);
        
        updateById(user);
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        if (id == 1L) {
            throw new BusinessException("不能删除超级管理员");
        }
        removeById(id);
    }
    
    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    /**
     * 修改密码（验证旧密码）
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少6个字符");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }
        return username.trim();
    }

    private void ensureUsernameNotExists(String username, Long tenantId, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getTenantId, tenantId)
            .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
    }

    private Long resolveTenantId(Long tenantId) {
        if (tenantId != null) {
            return tenantId;
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null) {
            return currentTenantId;
        }
        return 1L;
    }
}
