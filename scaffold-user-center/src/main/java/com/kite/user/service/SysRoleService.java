package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysRole;
import com.kite.user.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {
    
    /**
     * 分页查询角色
     */
    public PageResult<SysRole> pageRoles(int pageNum, int pageSize, String roleName, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
               .eq(status != null, SysRole::getStatus, status)
               .orderByAsc(SysRole::getSortOrder);
        
        Page<SysRole> page = page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
    
    /**
     * 新增角色
     */
    public void addRole(SysRole role) {
        // 检查角色编码是否存在
        long count = count(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleCode, role.getRoleCode()));
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
        }
        
        save(role);
    }
    
    /**
     * 更新角色
     */
    public void updateRole(SysRole role) {
        SysRole existRole = getById(role.getId());
        if (existRole == null) {
            throw new BusinessException("角色不存在");
        }
        
        // 如果修改了角色编码，检查是否重复
        if (!existRole.getRoleCode().equals(role.getRoleCode())) {
            long count = count(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode())
                .ne(SysRole::getId, role.getId()));
            if (count > 0) {
                throw new BusinessException("角色编码已存在");
            }
        }
        
        updateById(role);
    }
    
    /**
     * 删除角色
     */
    public void deleteRole(Long id) {
        if (id == 1L) {
            throw new BusinessException("不能删除超级管理员角色");
        }
        removeById(id);
    }
}
