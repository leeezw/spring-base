package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.dto.request.DeptRequests;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.entity.SysDept;
import com.kite.user.service.SysDeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

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

    @GetMapping("/tree")
    @RequiresPermissions("system:dept:query")
    public Result<List<SysDept>> tree() {
        return Result.success(deptService.getDeptTree());
    }

    @GetMapping("/list")
    @RequiresPermissions("system:dept:query")
    public Result<List<SysDept>> list() {
        return Result.success(deptService.list());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:dept:query")
    public Result<SysDept> getById(@PathVariable Long id) {
        return Result.success(deptService.getById(id));
    }

    @PostMapping
    @RequiresPermissions("system:dept:add")
    @OperationLog(module = "部门管理", type = OperationType.INSERT, description = "新增部门")
    public Result<Void> add(@Valid @RequestBody DeptRequests.Save request) {
        deptService.addDept(RequestConverters.toDept(request));
        return Result.success();
    }

    @PutMapping
    @RequiresPermissions("system:dept:edit")
    @OperationLog(module = "部门管理", type = OperationType.UPDATE, description = "更新部门")
    public Result<Void> update(@Valid @RequestBody DeptRequests.Update request) {
        deptService.updateDept(RequestConverters.toDept(request));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:dept:delete")
    @OperationLog(module = "部门管理", type = OperationType.DELETE, description = "删除部门")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success();
    }
}
