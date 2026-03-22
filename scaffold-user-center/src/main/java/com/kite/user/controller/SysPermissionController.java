package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.dto.request.PermissionRequests;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.entity.SysPermission;
import com.kite.user.service.SysPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/permission")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping("/page")
    @RequiresPermissions("system:permission:query")
    @OperationLog(module = "权限管理", type = OperationType.QUERY, description = "分页查询权限")
    public Result<PageResult<SysPermission>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String permissionName,
            @RequestParam(required = false) Integer status) {
        return Result.success(permissionService.pagePermissions(pageNum, pageSize, permissionName, status));
    }

    @GetMapping("/list")
    @RequiresPermissions("system:permission:query")
    public Result<List<SysPermission>> list() {
        return Result.success(permissionService.list());
    }

    @GetMapping("/tree")
    @RequiresPermissions("system:permission:query")
    public Result<List<SysPermission>> tree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:permission:query")
    public Result<SysPermission> getById(@PathVariable Long id) {
        return Result.success(permissionService.getById(id));
    }

    @PostMapping
    @RequiresPermissions("system:permission:add")
    @OperationLog(module = "权限管理", type = OperationType.INSERT, description = "新增权限")
    public Result<Void> add(@Valid @RequestBody PermissionRequests.Save request) {
        permissionService.addPermission(RequestConverters.toPermission(request));
        return Result.success();
    }

    @PutMapping
    @RequiresPermissions("system:permission:edit")
    @OperationLog(module = "权限管理", type = OperationType.UPDATE, description = "更新权限")
    public Result<Void> update(@Valid @RequestBody PermissionRequests.Update request) {
        permissionService.updatePermission(RequestConverters.toPermission(request));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:permission:delete")
    @OperationLog(module = "权限管理", type = OperationType.DELETE, description = "删除权限")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }
}
