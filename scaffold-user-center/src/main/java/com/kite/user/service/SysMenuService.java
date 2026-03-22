package com.kite.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kite.user.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> getUserMenuTree();

    List<SysMenu> getMenuTree();

    void addMenu(SysMenu menu);

    void updateMenu(SysMenu menu);

    void deleteMenu(Long id);
}
