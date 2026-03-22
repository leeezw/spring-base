package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.dto.request.RoleRequests;
import com.kite.user.entity.SysRole;
import com.kite.user.service.DataScopeService;
import com.kite.user.service.SysRelationService;
import com.kite.user.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;
    private final SysRelationService relationService;
    private final DataScopeService dataScopeService;

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

    @GetMapping("/list")
    @RequiresPermissions("system:role:query")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:role:query")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    @RequiresPermissions("system:role:add")
    @OperationLog(module = "角色管理", type = OperationType.INSERT, description = "新增角色")
    public Result<Map<String, Object>> add(@Valid @RequestBody RoleRequests.Create request) {
        SysRole role = RequestConverters.toRole(request);
        roleService.addRole(role);
        relationService.assignRolePermissions(role.getId(), request.getPermissionIds());
        dataScopeService.saveRoleDepts(role.getId(), request.getDataScope() != null && request.getDataScope() == 5 ? request.getCustomDeptIds() : List.of());
        Map<String, Object> data = new HashMap<>();
        data.put("id", role.getId());
        return Result.success(data);
    }

    @PutMapping
    @RequiresPermissions("system:role:edit")
    @OperationLog(module = "角色管理", type = OperationType.UPDATE, description = "更新角色")
    public Result<Void> update(@Valid @RequestBody RoleRequests.Update request) {
        SysRole role = RequestConverters.toRole(request);
        roleService.updateRole(role);
        relationService.assignRolePermissions(role.getId(), request.getPermissionIds());
        dataScopeService.saveRoleDepts(role.getId(), request.getDataScope() != null && request.getDataScope() == 5 ? request.getCustomDeptIds() : List.of());
        return Result.success();
    }

    @PutMapping("/status")
    @RequiresPermissions("system:role:edit")
    @OperationLog(module = "角色管理", type = OperationType.UPDATE, description = "修改角色状态")
    public Result<Void> updateStatus(@Valid @RequestBody RoleRequests.UpdateStatus request) {
        roleService.lambdaUpdate()
                .eq(SysRole::getId, request.getId())
                .set(SysRole::getStatus, request.getStatus())
                .update();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:role:delete")
    @OperationLog(module = "角色管理", type = OperationType.DELETE, description = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
}
