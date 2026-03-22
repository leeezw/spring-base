package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysDept;
import com.kite.user.entity.SysPost;
import com.kite.user.entity.SysRole;
import com.kite.user.entity.SysUser;
import com.kite.user.entity.SysUserPost;
import com.kite.user.entity.SysUserRole;
import com.kite.user.mapper.SysDeptMapper;
import com.kite.user.mapper.SysPostMapper;
import com.kite.user.mapper.SysRoleMapper;
import com.kite.user.mapper.SysUserMapper;
import com.kite.user.mapper.SysUserPostMapper;
import com.kite.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    public PageResult<SysUser> pageUsers(int pageNum, int pageSize, String username, String nickname, Integer status) {
        return pageUsers(pageNum, pageSize, username, nickname, status, null);
    }

    public PageResult<SysUser> pageUsers(int pageNum, int pageSize, String username, String nickname, Integer status, Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(nickname), SysUser::getNickname, nickname)
                .eq(status != null, SysUser::getStatus, status)
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .orderByDesc(SysUser::getCreateTime);

        DataScopeService.DataScope scope = dataScopeService.getDataScope();
        if (!scope.isAll()) {
            if (scope.isSelfOnly()) {
                wrapper.eq(SysUser::getId, scope.getUserId());
            } else if (scope.getDeptIds() != null && !scope.getDeptIds().isEmpty()) {
                if (deptId != null) {
                    if (!scope.getDeptIds().contains(deptId)) {
                        return new PageResult<>(Collections.emptyList(), 0L, (long) pageNum, (long) pageSize);
                    }
                } else {
                    wrapper.in(SysUser::getDeptId, scope.getDeptIds());
                }
            } else {
                wrapper.eq(SysUser::getId, scope.getUserId());
            }
        }

        Page<SysUser> page = page(new Page<>(pageNum, pageSize), wrapper);
        List<SysUser> records = page.getRecords();
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream().map(SysUser::getId).collect(Collectors.toList());
            Set<Long> deptIds = records.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());

            Map<Long, String> deptNameMap = new HashMap<>();
            if (!deptIds.isEmpty()) {
                List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
                depts.forEach(d -> deptNameMap.put(d.getId(), d.getDeptName()));
            }

            List<SysUserRole> allUserRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
            Map<Long, List<Long>> userRoleMap = allUserRoles.stream()
                    .collect(Collectors.groupingBy(SysUserRole::getUserId,
                            Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));

            Set<Long> allRoleIds = allUserRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, SysRole> roleInfoMap = new HashMap<>();
            if (!allRoleIds.isEmpty()) {
                List<SysRole> roles = roleMapper.selectBatchIds(allRoleIds);
                roles.forEach(r -> roleInfoMap.put(r.getId(), r));
            }

            List<SysUserPost> allUserPosts = userPostMapper.selectList(
                    new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, userIds));
            Map<Long, List<Long>> userPostMap = allUserPosts.stream()
                    .collect(Collectors.groupingBy(SysUserPost::getUserId,
                            Collectors.mapping(SysUserPost::getPostId, Collectors.toList())));

            Set<Long> allPostIds = allUserPosts.stream().map(SysUserPost::getPostId).collect(Collectors.toSet());
            Map<Long, SysPost> postInfoMap = new HashMap<>();
            if (!allPostIds.isEmpty()) {
                List<SysPost> posts = postMapper.selectBatchIds(allPostIds);
                posts.forEach(p -> postInfoMap.put(p.getId(), p));
            }

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

    public void addUser(SysUser user) {
        String username = normalizeUsername(user.getUsername());
        user.setUsername(username);
        user.setNickname(trimToNull(user.getNickname()));
        user.setEmail(trimToNull(user.getEmail()));
        user.setPhone(trimToNull(user.getPhone()));
        Long tenantId = resolveTenantId(user.getTenantId());
        user.setTenantId(tenantId);
        validateDept(user.getDeptId());
        ensureUsernameNotExists(username, tenantId, null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);
    }

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
        user.setNickname(trimToNull(user.getNickname()));
        user.setEmail(trimToNull(user.getEmail()));
        user.setPhone(trimToNull(user.getPhone()));
        Long tenantId = existUser.getTenantId();
        user.setTenantId(tenantId);
        validateDept(user.getDeptId());

        if (!existUser.getUsername().equals(newUsername)) {
            ensureUsernameNotExists(newUsername, tenantId, user.getId());
        }

        user.setPassword(null);
        updateById(user);
    }

    public void deleteUser(Long id) {
        if (id == 1L) {
            throw new BusinessException("不能删除超级管理员");
        }
        removeById(id);
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

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

    private void validateDept(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (deptMapper.selectById(deptId) == null) {
            throw new BusinessException("所属部门不存在");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
