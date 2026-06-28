package com.kite.cert.controller;

import com.kite.cert.dto.request.DnsProviderRequests;
import com.kite.cert.dto.response.DnsProviderResponse;
import com.kite.cert.service.CertDnsProviderService;
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
 * DNS 服务商管理接口。
 */
@RestController
@RequestMapping("/api/cert/dns-providers")
@RequiredArgsConstructor
public class CertDnsProviderController {

    private final CertDnsProviderService dnsProviderService;

    @GetMapping("/page")
    @RequiresPermissions("cert:dns:query")
    public Result<PageResult<DnsProviderResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(dnsProviderService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    @RequiresPermissions("cert:dns:query")
    public Result<List<DnsProviderResponse>> list() {
        return Result.success(dnsProviderService.listAll());
    }

    @GetMapping("/{id}")
    @RequiresPermissions("cert:dns:query")
    public Result<DnsProviderResponse> getById(@PathVariable Long id) {
        return Result.success(dnsProviderService.getDetail(id));
    }

    @PostMapping
    @RequiresPermissions("cert:dns:edit")
    @OperationLog(module = "DNS服务商", type = OperationType.INSERT, description = "新增DNS服务商")
    public Result<Void> add(@Valid @RequestBody DnsProviderRequests.Save request) {
        dnsProviderService.add(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("cert:dns:edit")
    @OperationLog(module = "DNS服务商", type = OperationType.UPDATE, description = "编辑DNS服务商")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DnsProviderRequests.Update request) {
        dnsProviderService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("cert:dns:edit")
    @OperationLog(module = "DNS服务商", type = OperationType.DELETE, description = "删除DNS服务商")
    public Result<Void> delete(@PathVariable Long id) {
        dnsProviderService.delete(id);
        return Result.success();
    }
}
