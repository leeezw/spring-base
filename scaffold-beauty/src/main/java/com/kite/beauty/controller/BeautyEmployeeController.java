package com.kite.beauty.controller;

import com.kite.beauty.entity.BeautyStore;
import com.kite.beauty.mapper.BeautyStoreMapper;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysEmployee;
import com.kite.user.service.SysEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beauty/employees")
@RequiredArgsConstructor
public class BeautyEmployeeController {

    private final SysEmployeeService employeeService;
    private final BeautyStoreMapper storeMapper;

    @GetMapping("/page")
    @RequiresPermissions("beauty:employee:query")
    public Result<PageResult<SysEmployee>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer empType,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Integer serviceEnabled) {
        return Result.success(employeeService.pageEmployees(pageNum, pageSize, keyword, deptId, status, empType, storeId, serviceEnabled));
    }

    @GetMapping("/select")
    @RequiresPermissions(value = {"beauty:employee:query", "hr:employee:query"}, logical = RequiresPermissions.Logical.OR)
    public Result<List<SysEmployee>> select(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "false") Boolean serviceOnly) {
        return Result.success(employeeService.listSelectableEmployees(storeId, serviceOnly));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("beauty:employee:query")
    public Result<SysEmployee> getById(@PathVariable Long id) {
        return Result.success(employeeService.getDetail(id));
    }

    @PostMapping
    @RequiresPermissions("beauty:employee:add")
    @OperationLog(module = "员工管理", type = OperationType.INSERT, description = "新增员工")
    public Result<Void> add(@RequestBody SysEmployee employee) {
        validateStore(employee.getStoreId());
        employeeService.addEmployee(employee);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("beauty:employee:edit")
    @OperationLog(module = "员工管理", type = OperationType.UPDATE, description = "编辑员工")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysEmployee employee) {
        employee.setId(id);
        validateStore(employee.getStoreId());
        employeeService.updateEmployee(employee);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("beauty:employee:delete")
    @OperationLog(module = "员工管理", type = OperationType.DELETE, description = "删除员工")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return Result.success();
    }

    @PostMapping("/{id}/create-account")
    @RequiresPermissions("beauty:employee:edit")
    @OperationLog(module = "员工管理", type = OperationType.INSERT, description = "开通账号")
    public Result<Map<String, String>> createAccount(@PathVariable Long id) {
        Map<String, String> info = employeeService.createAccount(id);
        return Result.success("账号开通成功，请妥善保存初始密码", info);
    }

    @DeleteMapping("/{id}/unbind-account")
    @RequiresPermissions("beauty:employee:edit")
    @OperationLog(module = "员工管理", type = OperationType.UPDATE, description = "解绑账号")
    public Result<Void> unbindAccount(@PathVariable Long id) {
        employeeService.unbindAccount(id);
        return Result.success();
    }

    private void validateStore(Long storeId) {
        if (storeId == null) {
            throw new BusinessException("所属门店不能为空");
        }
        BeautyStore store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException("所属门店不存在");
        }
        if (store.getStatus() != null && store.getStatus() == 0) {
            throw new BusinessException("停业门店不能新增或编辑员工");
        }
    }
}
