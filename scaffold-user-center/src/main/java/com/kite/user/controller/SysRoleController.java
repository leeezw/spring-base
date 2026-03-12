package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysRole;
import com.kite.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {
    
    private final SysRoleService roleService;
    
    /**
     * 分页查询角色
     */
    @GetMapping("/page")
    @RequiresPermissions("system:role:query")
    @OperationLog(module = "角色管理", type = OperationType.QUERY, description = "分页查询角色")
    public Result<PageResult<SysRole>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Integer status) {
        return Result.success(roleService.pageRoles(pageNum, pageSize, roleName, status));
    }
    
    /**
     * 查询所有角色
     */
    @GetMapping("/list")
    @RequiresPermissions("system:role:query")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }
    
    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    @RequiresPermissions("system:role:query")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }
    
    /**
     * 新增角色
     */
    @PostMapping
    @RequiresPermissions("system:role:add")
    @OperationLog(module = "角色管理", type = OperationType.INSERT, description = "新增角色")
    public Result<Void> add(@RequestBody SysRole role) {
        roleService.addRole(role);
        return Result.success();
    }
    
    /**
     * 更新角色
     */
    @PutMapping
    @RequiresPermissions("system:role:edit")
    @OperationLog(module = "角色管理", type = OperationType.UPDATE, description = "更新角色")
    public Result<Void> update(@RequestBody SysRole role) {
        roleService.updateRole(role);
        return Result.success();
    }
    
    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("system:role:delete")
    @OperationLog(module = "角色管理", type = OperationType.DELETE, description = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
}
