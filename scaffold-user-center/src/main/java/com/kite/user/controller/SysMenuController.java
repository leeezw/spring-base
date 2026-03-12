package com.kite.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.common.response.Result;
import com.kite.user.entity.SysMenu;
import com.kite.user.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理Controller
 */
@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {
    
    @Autowired
    private SysMenuService sysMenuService;
    
    /**
     * 获取当前用户的菜单树
     */
    @GetMapping("/user-menus")
    public Result<List<SysMenu>> getUserMenus() {
        List<SysMenu> menus = sysMenuService.getUserMenuTree();
        return Result.success(menus);
    }
    
    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Result<Page<SysMenu>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String menuName) {
        
        Page<SysMenu> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        
        if (menuName != null && !menuName.isEmpty()) {
            wrapper.like(SysMenu::getMenuName, menuName);
        }
        
        wrapper.orderByAsc(SysMenu::getSortOrder);
        
        return Result.success(sysMenuService.page(page, wrapper));
    }
    
    /**
     * 获取所有菜单（树形）
     */
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.success(sysMenuService.getMenuTree());
    }
    
    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(sysMenuService.getById(id));
    }
    
    /**
     * 新增
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysMenu menu) {
        sysMenuService.save(menu);
        return Result.success();
    }
    
    /**
     * 更新
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysMenu menu) {
        sysMenuService.updateById(menu);
        return Result.success();
    }
    
    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.removeById(id);
        return Result.success();
    }
}
