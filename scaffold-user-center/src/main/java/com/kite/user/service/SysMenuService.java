package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysMenu;
import com.kite.user.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务
 */
@Service
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {
    
    /**
     * 分页查询菜单
     */
    public PageResult<SysMenu> pageMenus(int pageNum, int pageSize, String menuName, Integer status) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(menuName)) {
            wrapper.like(SysMenu::getMenuName, menuName);
        }
        if (status != null) {
            wrapper.eq(SysMenu::getStatus, status);
        }
        
        wrapper.orderByAsc(SysMenu::getSortOrder);
        
        IPage<SysMenu> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }
    
    /**
     * 获取菜单树
     */
    public List<SysMenu> getMenuTree() {
        List<SysMenu> allMenus = this.list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));
        
        return buildMenuTree(allMenus, 0L);
    }
    
    /**
     * 构建菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        List<SysMenu> children = allMenus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .collect(Collectors.toList());
        
        for (SysMenu child : children) {
            List<SysMenu> subChildren = buildMenuTree(allMenus, child.getId());
            child.setChildren(subChildren);
        }
        
        return children;
    }
    
    /**
     * 新增菜单
     */
    public void addMenu(SysMenu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        this.save(menu);
    }
    
    /**
     * 更新菜单
     */
    public void updateMenu(SysMenu menu) {
        this.updateById(menu);
    }
    
    /**
     * 删除菜单
     */
    public void deleteMenu(Long id) {
        // 检查是否有子菜单
        long count = this.count(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (count > 0) {
            throw new RuntimeException("存在子菜单，无法删除");
        }
        this.removeById(id);
    }
}
