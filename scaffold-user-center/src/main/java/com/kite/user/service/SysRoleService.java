package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysRole;
import com.kite.user.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    public PageResult<SysRole> pageRoles(int pageNum, int pageSize, String roleName, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getSortOrder);

        Page<SysRole> page = page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public void addRole(SysRole role) {
        String roleCode = normalizeRoleCode(role.getRoleCode());
        role.setRoleCode(roleCode);
        role.setRoleName(role.getRoleName() == null ? null : role.getRoleName().trim());
        role.setDescription(trimToNull(role.getDescription()));
        Long tenantId = resolveTenantId(role.getTenantId());
        role.setTenantId(tenantId);
        if (role.getSortOrder() == null) {
            role.setSortOrder(0);
        }
        if (role.getDataScope() == null) {
            role.setDataScope(1);
        }
        ensureRoleCodeNotExists(roleCode, tenantId, null);
        save(role);
    }

    public void updateRole(SysRole role) {
        SysRole existRole = getById(role.getId());
        if (existRole == null) {
            throw new BusinessException("角色不存在");
        }

        String newRoleCode = role.getRoleCode();
        if (!StringUtils.hasText(newRoleCode)) {
            newRoleCode = existRole.getRoleCode();
        }
        newRoleCode = normalizeRoleCode(newRoleCode);
        role.setRoleCode(newRoleCode);
        role.setRoleName(role.getRoleName() == null ? null : role.getRoleName().trim());
        role.setDescription(trimToNull(role.getDescription()));
        Long tenantId = existRole.getTenantId();
        role.setTenantId(tenantId);
        if (role.getSortOrder() == null) {
            role.setSortOrder(existRole.getSortOrder());
        }
        if (role.getDataScope() == null) {
            role.setDataScope(existRole.getDataScope());
        }

        if (!existRole.getRoleCode().equals(newRoleCode)) {
            ensureRoleCodeNotExists(newRoleCode, tenantId, role.getId());
        }

        updateById(role);
    }

    public void deleteRole(Long id) {
        if (id == 1L) {
            throw new BusinessException("不能删除超级管理员角色");
        }
        removeById(id);
    }

    private String normalizeRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException("角色编码不能为空");
        }
        return roleCode.trim();
    }

    private void ensureRoleCodeNotExists(String roleCode, Long tenantId, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleCode, roleCode);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
