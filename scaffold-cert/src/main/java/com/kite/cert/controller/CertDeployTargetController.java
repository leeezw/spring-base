package com.kite.cert.controller;

import com.kite.cert.dto.request.DeployTargetRequests;
import com.kite.cert.dto.response.DeployTargetResponse;
import com.kite.cert.service.CertDeployTargetService;
import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import jakarta.validation.Valid;
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

/**
 * 部署目标管理接口。
 */
@RestController
@RequestMapping("/api/cert/deploy-targets")
@RequiredArgsConstructor
public class CertDeployTargetController {

    private final CertDeployTargetService deployTargetService;

    @GetMapping("/page")
    @RequiresPermissions("cert:deploy:query")
    public Result<PageResult<DeployTargetResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(deployTargetService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    @RequiresPermissions("cert:deploy:query")
    public Result<List<DeployTargetResponse>> list() {
        return Result.success(deployTargetService.listAll());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("cert:deploy:query")
    public Result<DeployTargetResponse> getById(@PathVariable Long id) {
        return Result.success(deployTargetService.getDetail(id));
    }

    @PostMapping
    @RequiresPermissions("cert:deploy:edit")
    @OperationLog(module = "部署目标", type = OperationType.INSERT, description = "新增部署目标")
    public Result<Void> add(@Valid @RequestBody DeployTargetRequests.Save request) {
        deployTargetService.add(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("cert:deploy:edit")
    @OperationLog(module = "部署目标", type = OperationType.UPDATE, description = "编辑部署目标")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeployTargetRequests.Update request) {
        deployTargetService.update(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    @RequiresPermissions("cert:deploy:edit")
    @OperationLog(module = "部署目标", type = OperationType.OTHER, description = "测试部署目标连通性")
    public Result<Void> test(@PathVariable Long id) {
        deployTargetService.test(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("cert:deploy:edit")
    @OperationLog(module = "部署目标", type = OperationType.DELETE, description = "删除部署目标")
    public Result<Void> delete(@PathVariable Long id) {
        deployTargetService.delete(id);
        return Result.success();
    }
}
