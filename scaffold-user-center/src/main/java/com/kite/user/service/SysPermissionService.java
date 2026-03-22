package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysPermission;
import com.kite.user.mapper.SysPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysPermissionService extends ServiceImpl<SysPermissionMapper, SysPermission> {

    private void addTenantCondition(LambdaQueryWrapper<SysPermission> wrapper) {
        Long tenantId = TenantContext.getTenantId();
        wrapper.in(SysPermission::getTenantId, tenantId == null ? Arrays.asList(0L) : Arrays.asList(0L, tenantId));
        if (tenantId != null && tenantId != 0L) {
            wrapper.notLike(SysPermission::getPermissionCode, "system:tenant");
        }
    }

    public PageResult<SysPermission> pagePermissions(int pageNum, int pageSize, String permissionName, Integer status) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        addTenantCondition(wrapper);

        if (StringUtils.hasText(permissionName)) {
            wrapper.like(SysPermission::getPermissionName, permissionName);
        }
        if (status != null) {
            wrapper.eq(SysPermission::getStatus, status);
        }

        wrapper.orderByAsc(SysPermission::getSortOrder);
        IPage<SysPermission> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public List<SysPermission> getPermissionTree() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        addTenantCondition(wrapper);
        wrapper.orderByAsc(SysPermission::getSortOrder);
        List<SysPermission> allPermissions = this.list(wrapper);
        return buildPermissionTree(allPermissions, 0L);
    }

    private List<SysPermission> buildPermissionTree(List<SysPermission> allPermissions, Long parentId) {
        return allPermissions.stream()
                .filter(permission -> parentId.equals(permission.getParentId()))
                .peek(permission -> {
                    List<SysPermission> children = buildPermissionTree(allPermissions, permission.getId());
                    permission.setChildren(children.isEmpty() ? null : children);
                })
                .collect(Collectors.toList());
    }

    public void addPermission(SysPermission permission) {
        permission.setPermissionCode(normalizePermissionCode(permission.getPermissionCode()));
        permission.setPermissionName(permission.getPermissionName() == null ? null : permission.getPermissionName().trim());
        permission.setPath(trimToNull(permission.getPath()));
        permission.setComponent(trimToNull(permission.getComponent()));
        permission.setIcon(trimToNull(permission.getIcon()));
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getSortOrder() == null) {
            permission.setSortOrder(0);
        }
        ensurePermissionCodeNotExists(permission.getPermissionCode(), null);
        validateParent(permission.getParentId(), null);
        this.save(permission);
    }

    public void updatePermission(SysPermission permission) {
        SysPermission existPermission = getById(permission.getId());
        if (existPermission == null) {
            throw new BusinessException("权限不存在");
        }
        permission.setPermissionCode(normalizePermissionCode(permission.getPermissionCode()));
        permission.setPermissionName(permission.getPermissionName() == null ? null : permission.getPermissionName().trim());
        permission.setPath(trimToNull(permission.getPath()));
        permission.setComponent(trimToNull(permission.getComponent()));
        permission.setIcon(trimToNull(permission.getIcon()));
        if (permission.getParentId() == null) {
            permission.setParentId(existPermission.getParentId());
        }
        if (permission.getSortOrder() == null) {
            permission.setSortOrder(existPermission.getSortOrder());
        }
        permission.setTenantId(existPermission.getTenantId());
        ensurePermissionCodeNotExists(permission.getPermissionCode(), permission.getId());
        validateParent(permission.getParentId(), permission.getId());
        this.updateById(permission);
    }

    public void deletePermission(Long id) {
        long childrenCount = count(new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getParentId, id));
        if (childrenCount > 0) {
            throw new BusinessException("存在子权限，无法删除");
        }
        this.removeById(id);
    }

    private String normalizePermissionCode(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            throw new BusinessException("权限编码不能为空");
        }
        return permissionCode.trim();
    }

    private void ensurePermissionCodeNotExists(String permissionCode, Long excludeId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, permissionCode);
        if (excludeId != null) {
            wrapper.ne(SysPermission::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }
    }

    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (currentId != null && parentId.equals(currentId)) {
            throw new BusinessException("上级权限不能选择自己");
        }
        SysPermission parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException("上级权限不存在");
        }
        Long cursor = parent.getParentId();
        while (cursor != null && cursor != 0L) {
            if (currentId != null && cursor.equals(currentId)) {
                throw new BusinessException("上级权限不能选择当前权限的子节点");
            }
            SysPermission current = getById(cursor);
            if (current == null) {
                break;
            }
            cursor = current.getParentId();
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
