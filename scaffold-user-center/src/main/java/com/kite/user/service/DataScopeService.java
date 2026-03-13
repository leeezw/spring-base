package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.user.entity.SysDept;
import com.kite.user.entity.SysRole;
import com.kite.user.entity.SysRoleDept;
import com.kite.user.entity.SysUserRole;
import com.kite.user.mapper.SysDeptMapper;
import com.kite.user.mapper.SysRoleMapper;
import com.kite.user.mapper.SysRoleDeptMapper;
import com.kite.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限服务
 * 根据当前用户的角色数据权限范围，生成可访问的部门ID列表
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataScopeService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 数据权限范围常量
     */
    public static final int SCOPE_ALL = 1;           // 全部数据
    public static final int SCOPE_DEPT = 2;           // 本部门
    public static final int SCOPE_DEPT_AND_CHILD = 3; // 本部门及下级
    public static final int SCOPE_SELF = 4;           // 仅本人
    public static final int SCOPE_CUSTOM = 5;         // 自定义部门

    /**
     * 数据权限过滤结果
     */
    public static class DataScope {
        /** 是否全部可见（无需过滤） */
        private final boolean all;
        /** 可访问的部门ID集合（SCOPE_DEPT/DEPT_AND_CHILD/CUSTOM时有值） */
        private final Set<Long> deptIds;
        /** 是否仅本人数据 */
        private final boolean selfOnly;
        /** 当前用户ID */
        private final Long userId;
        /** 当前用户部门ID */
        private final Long deptId;

        public DataScope(boolean all, Set<Long> deptIds, boolean selfOnly, Long userId, Long deptId) {
            this.all = all;
            this.deptIds = deptIds;
            this.selfOnly = selfOnly;
            this.userId = userId;
            this.deptId = deptId;
        }

        public boolean isAll() { return all; }
        public Set<Long> getDeptIds() { return deptIds; }
        public boolean isSelfOnly() { return selfOnly; }
        public Long getUserId() { return userId; }
        public Long getDeptId() { return deptId; }
    }

    /**
     * 获取当前用户的数据权限范围
     * 取所有角色中权限最大的那个（数字越小权限越大）
     */
    public DataScope getDataScope() {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return new DataScope(false, Collections.emptySet(), true, null, null);
        }

        Long userId = loginUser.getUserId();
        Long userDeptId = loginUser.getDeptId();

        // 超级管理员直接全部
        if (userId != null && userId == 1L) {
            return new DataScope(true, null, false, userId, userDeptId);
        }

        // 查用户所有角色
        List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return new DataScope(false, Collections.emptySet(), true, userId, userDeptId);
        }

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .eq(SysRole::getDeleted, 0)
        );

        if (roles.isEmpty()) {
            return new DataScope(false, Collections.emptySet(), true, userId, userDeptId);
        }

        // 取权限最大的scope（数字最小）
        int minScope = roles.stream()
            .mapToInt(r -> r.getDataScope() != null ? r.getDataScope() : SCOPE_ALL)
            .min().orElse(SCOPE_SELF);

        switch (minScope) {
            case SCOPE_ALL:
                return new DataScope(true, null, false, userId, userDeptId);

            case SCOPE_DEPT:
                if (userDeptId == null) {
                    return new DataScope(false, Collections.emptySet(), true, userId, userDeptId);
                }
                return new DataScope(false, Set.of(userDeptId), false, userId, userDeptId);

            case SCOPE_DEPT_AND_CHILD:
                if (userDeptId == null) {
                    return new DataScope(false, Collections.emptySet(), true, userId, userDeptId);
                }
                Set<Long> deptAndChildren = getDeptAndChildren(userDeptId);
                return new DataScope(false, deptAndChildren, false, userId, userDeptId);

            case SCOPE_SELF:
                return new DataScope(false, Collections.emptySet(), true, userId, userDeptId);

            case SCOPE_CUSTOM:
                // 合并所有角色的自定义部门
                Set<Long> customDeptIds = new HashSet<>();
                for (SysRole role : roles) {
                    if (role.getDataScope() != null && role.getDataScope() == SCOPE_CUSTOM) {
                        List<SysRoleDept> rds = roleDeptMapper.selectList(
                            new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getId())
                        );
                        rds.forEach(rd -> customDeptIds.add(rd.getDeptId()));
                    }
                }
                if (customDeptIds.isEmpty()) {
                    return new DataScope(false, Collections.emptySet(), true, userId, userDeptId);
                }
                return new DataScope(false, customDeptIds, false, userId, userDeptId);

            default:
                return new DataScope(true, null, false, userId, userDeptId);
        }
    }

    /**
     * 获取部门及所有子部门ID
     */
    private Set<Long> getDeptAndChildren(Long deptId) {
        Set<Long> result = new HashSet<>();
        result.add(deptId);

        List<SysDept> allDepts = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0)
        );

        // BFS查子部门
        Queue<Long> queue = new LinkedList<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            for (SysDept dept : allDepts) {
                if (parentId.equals(dept.getParentId()) && !result.contains(dept.getId())) {
                    result.add(dept.getId());
                    queue.add(dept.getId());
                }
            }
        }
        return result;
    }

    /**
     * 获取角色关联的自定义部门ID列表
     */
    public List<Long> getRoleDeptIds(Long roleId) {
        List<SysRoleDept> list = roleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId)
        );
        return list.stream().map(SysRoleDept::getDeptId).collect(Collectors.toList());
    }

    /**
     * 保存角色关联的自定义部门
     */
    public void saveRoleDepts(Long roleId, List<Long> deptIds) {
        // 删除旧的
        roleDeptMapper.delete(
            new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId)
        );
        // 插入新的
        if (deptIds != null) {
            for (Long deptId : deptIds) {
                roleDeptMapper.insert(new SysRoleDept(roleId, deptId));
            }
        }
    }
}
