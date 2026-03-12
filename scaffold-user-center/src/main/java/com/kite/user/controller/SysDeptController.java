package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysDept;
import com.kite.user.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理
 */
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {
    
    private final SysDeptService deptService;
    
    /**
     * 分页查询部门
     */
    @GetMapping("/page")
    @RequiresPermissions("system:dept:query")
    @OperationLog(module = "部门管理", type = OperationType.QUERY, description = "分页查询部门")
    public Result<PageResult<SysDept>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) Integer status) {
        return Result.success(deptService.pageDepts(pageNum, pageSize, deptName, status));
    }
    
    /**
     * 查询所有部门(树形结构)
     */
    @GetMapping("/tree")
    @RequiresPermissions("system:dept:query")
    public Result<List<SysDept>> tree() {
        return Result.success(deptService.getDeptTree());
    }
    
    /**
     * 查询所有部门(列表)
     */
    @GetMapping("/list")
    @RequiresPermissions("system:dept:query")
    public Result<List<SysDept>> list() {
        return Result.success(deptService.list());
    }
    
    /**
     * 根据ID查询部门
     */
    @GetMapping("/{id}")
    @RequiresPermissions("system:dept:query")
    public Result<SysDept> getById(@PathVariable Long id) {
        return Result.success(deptService.getById(id));
    }
    
    /**
     * 新增部门
     */
    @PostMapping
    @RequiresPermissions("system:dept:add")
    @OperationLog(module = "部门管理", type = OperationType.INSERT, description = "新增部门")
    public Result<Void> add(@RequestBody SysDept dept) {
        deptService.addDept(dept);
        return Result.success();
    }
    
    /**
     * 更新部门
     */
    @PutMapping
    @RequiresPermissions("system:dept:edit")
    @OperationLog(module = "部门管理", type = OperationType.UPDATE, description = "更新部门")
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.updateDept(dept);
        return Result.success();
    }
    
    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("system:dept:delete")
    @OperationLog(module = "部门管理", type = OperationType.DELETE, description = "删除部门")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success();
    }
}
