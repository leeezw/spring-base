package com.kite.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kite.user.entity.SysMenu;

import java.util.List;

/**
 * 菜单Service
 */
public interface SysMenuService extends IService<SysMenu> {
    
    /**
     * 获取当前用户的菜单树
     */
    List<SysMenu> getUserMenuTree();
    
    /**
     * 获取所有菜单树
     */
    List<SysMenu> getMenuTree();
}
