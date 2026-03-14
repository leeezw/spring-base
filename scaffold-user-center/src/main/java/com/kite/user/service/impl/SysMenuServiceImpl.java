package com.kite.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.auth.model.LoginUserContext;
import com.kite.auth.model.LoginUser;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysMenu;
import com.kite.user.mapper.SysMenuMapper;
import com.kite.user.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单ServiceImpl
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    
    @Override
    public List<SysMenu> getUserMenuTree() {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return new ArrayList<>();
        }
        
        // 获取用户的菜单ID列表
        List<Long> menuIds = baseMapper.selectMenuIdsByUserId(loginUser.getUserId());
        
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询菜单列表（菜单已忽略租户过滤，通过角色关联自然隔离）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getId, menuIds)
               .eq(SysMenu::getStatus, 1)
               .eq(SysMenu::getVisible, 1)
               .orderByAsc(SysMenu::getSortOrder);
        
        List<SysMenu> allMenus = list(wrapper);
        
        // 构建树形结构
        return buildMenuTree(allMenus, 0L);
    }
    
    @Override
    public List<SysMenu> getMenuTree() {
        Long tenantId = TenantContext.getTenantId();
        
        // 手动过滤：系统内置(tenant_id=0) + 当前租户的自建菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getTenantId, tenantId == null ? Arrays.asList(0L) : Arrays.asList(0L, tenantId))
               .orderByAsc(SysMenu::getSortOrder);
        
        List<SysMenu> allMenus = list(wrapper);
        return buildMenuTree(allMenus, 0L);
    }
    
    /**
     * 构建菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .peek(menu -> {
                    List<SysMenu> children = buildMenuTree(allMenus, menu.getId());
                    menu.setChildren(children.isEmpty() ? null : children);
                })
                .collect(Collectors.toList());
    }
}
