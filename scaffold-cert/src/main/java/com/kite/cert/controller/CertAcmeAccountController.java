package com.kite.cert.controller;

import com.kite.cert.dto.request.AcmeAccountRequests;
import com.kite.cert.dto.response.AcmeAccountResponse;
import com.kite.cert.service.CertAcmeAccountService;
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
 * ACME 账户管理接口。
 */
@RestController
@RequestMapping("/api/cert/accounts")
@RequiredArgsConstructor
public class CertAcmeAccountController {

    private final CertAcmeAccountService accountService;

    @GetMapping("/page")
    @RequiresPermissions("cert:account:query")
    public Result<PageResult<AcmeAccountResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(accountService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    @RequiresPermissions("cert:account:query")
    public Result<List<AcmeAccountResponse>> list() {
        return Result.success(accountService.listAll());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("cert:account:query")
    public Result<AcmeAccountResponse> getById(@PathVariable Long id) {
        return Result.success(accountService.getDetail(id));
    }

    @PostMapping
    @RequiresPermissions("cert:account:edit")
    @OperationLog(module = "ACME账户", type = OperationType.INSERT, description = "新增ACME账户")
    public Result<Void> add(@Valid @RequestBody AcmeAccountRequests.Save request) {
        accountService.add(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("cert:account:edit")
    @OperationLog(module = "ACME账户", type = OperationType.UPDATE, description = "编辑ACME账户")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AcmeAccountRequests.Update request) {
        accountService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("cert:account:edit")
    @OperationLog(module = "ACME账户", type = OperationType.DELETE, description = "删除ACME账户")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Result.success();
    }
}
