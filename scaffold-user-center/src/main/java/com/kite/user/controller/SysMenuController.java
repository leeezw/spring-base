package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysMenu;
import com.kite.user.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 */
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {
    
    private final SysMenuService menuService;
    
    /**
     * 分页查询菜单
     */
    @GetMapping("/page")
    @RequiresPermissions("system:menu:query")
    @OperationLog(module = "菜单管理", type = OperationType.QUERY, description = "分页查询菜单")
    public Result<PageResult<SysMenu>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) Integer status) {
        return Result.success(menuService.pageMenus(pageNum, pageSize, menuName, status));
    }
    
    /**
     * 查询所有菜单(树形结构)
     */
    @GetMapping("/tree")
    @RequiresPermissions("system:menu:query")
    public Result<List<SysMenu>> tree() {
        return Result.success(menuService.getMenuTree());
    }
    
    /**
     * 查询所有菜单(列表)
     */
    @GetMapping("/list")
    @RequiresPermissions("system:menu:query")
    public Result<List<SysMenu>> list() {
        return Result.success(menuService.list());
    }
    
    /**
     * 根据ID查询菜单
     */
    @GetMapping("/{id}")
    @RequiresPermissions("system:menu:query")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(menuService.getById(id));
    }
    
    /**
     * 新增菜单
     */
    @PostMapping
    @RequiresPermissions("system:menu:add")
    @OperationLog(module = "菜单管理", type = OperationType.INSERT, description = "新增菜单")
    public Result<Void> add(@RequestBody SysMenu menu) {
        menuService.addMenu(menu);
        return Result.success();
    }
    
    /**
     * 更新菜单
     */
    @PutMapping
    @RequiresPermissions("system:menu:edit")
    @OperationLog(module = "菜单管理", type = OperationType.UPDATE, description = "更新菜单")
    public Result<Void> update(@RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return Result.success();
    }
    
    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("system:menu:delete")
    @OperationLog(module = "菜单管理", type = OperationType.DELETE, description = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }
}
