package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysPermission;
import com.kite.user.mapper.SysPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务
 */
@Service
public class SysPermissionService extends ServiceImpl<SysPermissionMapper, SysPermission> {
    
    /**
     * 手动添加租户条件：系统内置(tenant_id=0) + 当前租户
     */
    private void addTenantCondition(LambdaQueryWrapper<SysPermission> wrapper) {
        Long tenantId = TenantContext.getTenantId();
        wrapper.in(SysPermission::getTenantId, tenantId == null ? Arrays.asList(0L) : Arrays.asList(0L, tenantId));
    }
    
    /**
     * 分页查询权限
     */
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
    
    /**
     * 获取权限树
     */
    public List<SysPermission> getPermissionTree() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        addTenantCondition(wrapper);
        wrapper.orderByAsc(SysPermission::getSortOrder);
        
        List<SysPermission> allPermissions = this.list(wrapper);
        return buildPermissionTree(allPermissions, 0L);
    }
    
    /**
     * 构建权限树
     */
    private List<SysPermission> buildPermissionTree(List<SysPermission> allPermissions, Long parentId) {
        return allPermissions.stream()
                .filter(permission -> parentId.equals(permission.getParentId()))
                .peek(permission -> {
                    List<SysPermission> children = buildPermissionTree(allPermissions, permission.getId());
                    permission.setChildren(children.isEmpty() ? null : children);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 新增权限
     */
    public void addPermission(SysPermission permission) {
        this.save(permission);
    }
    
    /**
     * 更新权限
     */
    public void updatePermission(SysPermission permission) {
        this.updateById(permission);
    }
    
    /**
     * 删除权限
     */
    public void deletePermission(Long id) {
        this.removeById(id);
    }
}
