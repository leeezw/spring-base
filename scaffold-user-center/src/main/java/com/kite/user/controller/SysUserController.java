package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysUser;
import com.kite.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {
    
    private final SysUserService userService;
    
    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    @RequiresPermissions("system:user:query")
    @OperationLog(module = "用户管理", type = OperationType.QUERY, description = "分页查询用户")
    public Result<PageResult<SysUser>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId) {
        return Result.success(userService.pageUsers(pageNum, pageSize, username, nickname, status, deptId));
    }
    
    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    @RequiresPermissions("system:user:query")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
    
    /**
     * 新增用户
     */
    @PostMapping
    @RequiresPermissions("system:user:add")
    @OperationLog(module = "用户管理", type = OperationType.INSERT, description = "新增用户")
    public Result<Map<String, Object>> add(@RequestBody SysUser user) {
        userService.addUser(user);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", user.getId());
        return Result.success(data);
    }
    
    /**
     * 更新用户
     */
    @PutMapping
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "更新用户")
    public Result<Void> update(@RequestBody SysUser user) {
        userService.updateUser(user);
        return Result.success();
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("system:user:delete")
    @OperationLog(module = "用户管理", type = OperationType.DELETE, description = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
    
    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }
}
