package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysTenant;
import com.kite.user.service.SysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理
 */
@RestController
@RequestMapping("/api/system/tenant")
@RequiredArgsConstructor
public class SysTenantController {
    
    private final SysTenantService tenantService;
    
    /**
     * 分页查询租户
     */
    @GetMapping("/page")
    @RequiresPermissions("system:tenant:query")
    @OperationLog(module = "租户管理", type = OperationType.QUERY, description = "分页查询租户")
    public Result<PageResult<SysTenant>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(tenantService.page(pageNum, pageSize, keyword));
    }
    
    /**
     * 查询所有租户
     */
    @GetMapping("/list")
    @RequiresPermissions("system:tenant:query")
    @OperationLog(module = "租户管理", type = OperationType.QUERY, description = "查询所有租户")
    public Result<List<SysTenant>> list() {
        return Result.success(tenantService.list());
    }
    
    /**
     * 根据ID查询租户
     */
    @GetMapping("/{id}")
    @RequiresPermissions("system:tenant:query")
    @OperationLog(module = "租户管理", type = OperationType.QUERY, description = "查询租户详情")
    public Result<SysTenant> getById(@PathVariable Long id) {
        return Result.success(tenantService.getById(id));
    }
    
    /**
     * 新增租户
     */
    @PostMapping
    @RequiresPermissions("system:tenant:add")
    @OperationLog(module = "租户管理", type = OperationType.INSERT, description = "新增租户")
    public Result<Void> save(@RequestBody SysTenant tenant) {
        // 检查租户编码是否已存在
        SysTenant existing = tenantService.getByCode(tenant.getTenantCode());
        if (existing != null) {
            return Result.fail("租户编码已存在");
        }
        
        tenantService.save(tenant);
        return Result.success();
    }
    
    /**
     * 更新租户
     */
    @PutMapping
    @RequiresPermissions("system:tenant:edit")
    @OperationLog(module = "租户管理", type = OperationType.UPDATE, description = "更新租户")
    public Result<Void> update(@RequestBody SysTenant tenant) {
        // 检查租户编码是否被其他租户使用
        SysTenant existing = tenantService.getByCode(tenant.getTenantCode());
        if (existing != null && !existing.getId().equals(tenant.getId())) {
            return Result.fail("租户编码已被使用");
        }
        
        tenantService.update(tenant);
        return Result.success();
    }
    
    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("system:tenant:delete")
    @OperationLog(module = "租户管理", type = OperationType.DELETE, description = "删除租户")
    public Result<Void> delete(@PathVariable Long id) {
        // 不允许删除默认租户
        if (id == 1L) {
            return Result.fail("不允许删除默认租户");
        }
        
        tenantService.delete(id);
        return Result.success();
    }
}
