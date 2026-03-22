package com.kite.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.common.response.Result;
import com.kite.user.dto.request.MenuRequests;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.entity.SysMenu;
import com.kite.user.service.SysMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/user-menus")
    public Result<List<SysMenu>> getUserMenus() {
        return Result.success(sysMenuService.getUserMenuTree());
    }

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

    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.success(sysMenuService.getMenuTree());
    }

    @GetMapping("/{id}")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(sysMenuService.getById(id));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody MenuRequests.Save request) {
        sysMenuService.addMenu(RequestConverters.toMenu(request));
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody MenuRequests.Update request) {
        sysMenuService.updateMenu(RequestConverters.toMenu(request));
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.deleteMenu(id);
        return Result.success();
    }
}
