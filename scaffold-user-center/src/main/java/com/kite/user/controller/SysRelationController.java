package com.kite.user.controller;

import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.service.SysRelationService;
import com.kite.user.service.SysPostService;
import com.kite.user.service.DataScopeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关联关系管理
 */
@RestController
@RequestMapping("/api/system/relation")
@RequiredArgsConstructor
public class SysRelationController {
    
    private final SysRelationService relationService;
    private final SysPostService postService;
    private final DataScopeService dataScopeService;
    
    /**
     * 查询用户的角色ID列表
     */
    @GetMapping("/user/{userId}/roles")
    @RequiresPermissions("system:user:query")
    public Result<List<Long>> getUserRoles(@PathVariable Long userId) {
        return Result.success(relationService.getUserRoleIds(userId));
    }
    
    /**
     * 分配用户角色
     */
    @PostMapping("/user/{userId}/roles")
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "分配用户角色")
    public Result<Void> assignUserRoles(@PathVariable Long userId, @RequestBody AssignRequest request) {
        relationService.assignUserRoles(userId, request.getIds());
        return Result.success();
    }
    
    /**
     * 查询角色的权限ID列表
     */
    @GetMapping("/role/{roleId}/permissions")
    @RequiresPermissions("system:role:query")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(relationService.getRolePermissionIds(roleId));
    }
    
    /**
     * 分配角色权限
     */
    @PostMapping("/role/{roleId}/permissions")
    @RequiresPermissions("system:role:edit")
    @OperationLog(module = "角色管理", type = OperationType.UPDATE, description = "分配角色权限")
    public Result<Void> assignRolePermissions(@PathVariable Long roleId, @RequestBody AssignRequest request) {
        relationService.assignRolePermissions(roleId, request.getIds());
        return Result.success();
    }
    
    /**
     * 查询角色的菜单ID列表
     */
    @GetMapping("/role/{roleId}/menus")
    @RequiresPermissions("system:role:query")
    public Result<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        return Result.success(relationService.getRoleMenuIds(roleId));
    }
    
    /**
     * 分配角色菜单
     */
    @PostMapping("/role/{roleId}/menus")
    @RequiresPermissions("system:role:edit")
    @OperationLog(module = "角色管理", type = OperationType.UPDATE, description = "分配角色菜单")
    public Result<Void> assignRoleMenus(@PathVariable Long roleId, @RequestBody AssignRequest request) {
        relationService.assignRoleMenus(roleId, request.getIds());
        return Result.success();
    }
    
    @Data
    public static class AssignRequest {
        private List<Long> ids;
    }

    // ============ 用户-岗位 ============

    /**
     * 查询用户的岗位ID列表
     */
    @GetMapping("/user/{userId}/posts")
    @RequiresPermissions("system:user:query")
    public Result<List<Long>> getUserPosts(@PathVariable Long userId) {
        return Result.success(postService.getUserPostIds(userId));
    }

    /**
     * 分配用户岗位
     */
    @PostMapping("/user/{userId}/posts")
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "分配用户岗位")
    public Result<Void> assignUserPosts(@PathVariable Long userId, @RequestBody AssignRequest request) {
        postService.assignUserPosts(userId, request.getIds());
        return Result.success();
    }

    // ========== 角色-部门（数据权限） ==========

    /**
     * 查询角色的自定义部门ID列表
     */
    @GetMapping("/role/{roleId}/depts")
    @RequiresPermissions("system:role:query")
    public Result<List<Long>> getRoleDepts(@PathVariable Long roleId) {
        return Result.success(dataScopeService.getRoleDeptIds(roleId));
    }

    /**
     * 设置角色的自定义部门
     */
    @PostMapping("/role/{roleId}/depts")
    @RequiresPermissions("system:role:edit")
    @OperationLog(module = "角色管理", type = OperationType.UPDATE, description = "设置角色数据权限部门")
    public Result<Void> assignRoleDepts(@PathVariable Long roleId, @RequestBody AssignRequest request) {
        dataScopeService.saveRoleDepts(roleId, request.getIds());
        return Result.success();
    }
}
