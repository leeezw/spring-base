package com.kite.user.controller;

import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.dto.request.UserRequests;
import com.kite.user.entity.SysUser;
import com.kite.user.service.SysPostService;
import com.kite.user.service.SysRelationService;
import com.kite.user.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final SysRelationService relationService;
    private final SysPostService postService;

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

    @GetMapping("/{id}")
    @RequiresPermissions("system:user:query")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PostMapping
    @RequiresPermissions("system:user:add")
    @OperationLog(module = "用户管理", type = OperationType.INSERT, description = "新增用户")
    public Result<Map<String, Object>> add(@Valid @RequestBody UserRequests.Create request) {
        SysUser user = RequestConverters.toUser(request);
        userService.addUser(user);
        relationService.assignUserRoles(user.getId(), request.getRoleIds());
        postService.assignUserPosts(user.getId(), request.getPostIds());
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        return Result.success(data);
    }

    @PutMapping
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "更新用户")
    public Result<Void> update(@Valid @RequestBody UserRequests.Update request) {
        SysUser user = RequestConverters.toUser(request);
        userService.updateUser(user);
        relationService.assignUserRoles(user.getId(), request.getRoleIds());
        postService.assignUserPosts(user.getId(), request.getPostIds());
        return Result.success();
    }

    @PutMapping("/status")
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "修改用户状态")
    public Result<Void> updateStatus(@Valid @RequestBody UserRequests.UpdateStatus request) {
        userService.lambdaUpdate()
                .eq(SysUser::getId, request.getId())
                .set(SysUser::getStatus, request.getStatus())
                .update();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:user:delete")
    @OperationLog(module = "用户管理", type = OperationType.DELETE, description = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @PutMapping("/batch-status")
    @RequiresPermissions("system:user:edit")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "批量修改状态")
    public Result<Void> batchUpdateStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        Integer status = (Integer) body.get("status");
        if (ids == null || ids.isEmpty() || status == null) {
            return Result.fail("参数错误");
        }
        userService.lambdaUpdate()
                .in(SysUser::getId, ids.stream().map(Number::longValue).toList())
                .set(SysUser::getStatus, status)
                .update();
        return Result.success();
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("system:user:delete")
    @OperationLog(module = "用户管理", type = OperationType.DELETE, description = "批量删除用户")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("参数错误");
        }
        if (ids.contains(1L)) {
            throw new BusinessException("不能删除超级管理员");
        }
        userService.removeByIds(ids);
        return Result.success();
    }
}
