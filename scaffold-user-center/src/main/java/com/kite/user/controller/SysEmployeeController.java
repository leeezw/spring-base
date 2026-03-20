package com.kite.user.controller;

import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysEmployee;
import com.kite.user.entity.SysEmployeeField;
import com.kite.user.service.SysEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 员工档案管理
 */
@RestController
@RequestMapping("/api/hr/employee")
@RequiredArgsConstructor
public class SysEmployeeController {

    private final SysEmployeeService employeeService;

    // ── 员工 CRUD ─────────────────────────────────────────────

    /**
     * 分页查询员工
     */
    @GetMapping("/page")
    @RequiresPermissions("hr:employee:query")
    public Result<PageResult<SysEmployee>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer empType) {
        return Result.success(employeeService.pageEmployees(pageNum, pageSize, keyword, deptId, status, empType));
    }

    /**
     * 查询员工详情
     */
    @GetMapping("/{id}")
    @RequiresPermissions("hr:employee:query")
    public Result<SysEmployee> getById(@PathVariable Long id) {
        return Result.success(employeeService.getDetail(id));
    }

    /**
     * 新增员工
     */
    @PostMapping
    @RequiresPermissions("hr:employee:add")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.INSERT, description = "新增员工")
    public Result<Void> add(@RequestBody SysEmployee employee) {
        employeeService.addEmployee(employee);
        return Result.success();
    }

    /**
     * 编辑员工
     */
    @PutMapping
    @RequiresPermissions("hr:employee:edit")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.UPDATE, description = "编辑员工")
    public Result<Void> update(@RequestBody SysEmployee employee) {
        employeeService.updateEmployee(employee);
        return Result.success();
    }

    /**
     * 删除员工
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("hr:employee:delete")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.DELETE, description = "删除员工")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return Result.success();
    }

    // ── 账号绑定 ──────────────────────────────────────────────

    /**
     * 开通账号：自动创建 SysUser 并绑定
     * 返回初始用户名和默认密码（仅此一次展示，请提示用户保存）
     */
    @PostMapping("/{id}/create-account")
    @RequiresPermissions("hr:employee:edit")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.INSERT, description = "开通账号")
    public Result<Map<String, String>> createAccount(@PathVariable Long id) {
        Map<String, String> info = employeeService.createAccount(id);
        return Result.success("账号开通成功，请妥善保存初始密码", info);
    }

    /**
     * 绑定已有账号
     * 请求体：{ "userId": 123 }
     */
    @PutMapping("/{id}/bind-account")
    @RequiresPermissions("hr:employee:edit")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.UPDATE, description = "绑定账号")
    public Result<Void> bindAccount(@PathVariable Long id,
                                    @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null) {
            return Result.fail("userId 不能为空");
        }
        employeeService.bindAccount(id, userId);
        return Result.success();
    }

    /**
     * 解绑账号（仅解除关联，不删除账号）
     */
    @DeleteMapping("/{id}/unbind-account")
    @RequiresPermissions("hr:employee:edit")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.UPDATE, description = "解绑账号")
    public Result<Void> unbindAccount(@PathVariable Long id) {
        employeeService.unbindAccount(id);
        return Result.success();
    }

    // ── 自定义字段定义管理 ────────────────────────────────────

    /**
     * 获取当前租户的自定义字段定义列表
     */
    @GetMapping("/fields")
    @RequiresPermissions("hr:employee:query")
    public Result<List<SysEmployeeField>> listFields() {
        return Result.success(employeeService.listFields());
    }

    /**
     * 新增自定义字段定义
     */
    @PostMapping("/fields")
    @RequiresPermissions("hr:employee:edit")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.INSERT, description = "新增自定义字段")
    public Result<Void> addField(@RequestBody SysEmployeeField field) {
        employeeService.addField(field);
        return Result.success();
    }

    /**
     * 编辑自定义字段定义（fieldKey 不可修改）
     */
    @PutMapping("/fields")
    @RequiresPermissions("hr:employee:edit")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.UPDATE, description = "编辑自定义字段")
    public Result<Void> updateField(@RequestBody SysEmployeeField field) {
        employeeService.updateField(field);
        return Result.success();
    }

    /**
     * 删除自定义字段定义
     */
    @DeleteMapping("/fields/{fieldId}")
    @RequiresPermissions("hr:employee:edit")
    @OperationLog(module = "员工管理", type = OperationLog.OperationType.DELETE, description = "删除自定义字段")
    public Result<Void> deleteField(@PathVariable Long fieldId) {
        employeeService.deleteField(fieldId);
        return Result.success();
    }
}
