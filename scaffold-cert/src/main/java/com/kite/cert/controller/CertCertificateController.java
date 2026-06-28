package com.kite.cert.controller;

import com.kite.cert.dto.request.CertificateRequests;
import com.kite.cert.dto.response.CertificateResponse;
import com.kite.cert.service.CertCertificateService;
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
import java.util.Map;

/**
 * 证书管理接口。
 */
@RestController
@RequestMapping("/api/cert/certificates")
@RequiredArgsConstructor
public class CertCertificateController {

    private final CertCertificateService certificateService;

    @GetMapping("/page")
    @RequiresPermissions("cert:certificate:query")
    public Result<PageResult<CertificateResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(certificateService.page(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("cert:certificate:query")
    public Result<CertificateResponse> getById(@PathVariable Long id) {
        return Result.success(certificateService.getDetail(id));
    }

    @PostMapping("/issue")
    @RequiresPermissions("cert:certificate:add")
    @OperationLog(module = "证书管理", type = OperationType.INSERT, description = "签发证书")
    public Result<Long> issue(@Valid @RequestBody CertificateRequests.Issue request) {
        return Result.success(certificateService.createAndIssue(request));
    }

    @PostMapping("/{id}/renew")
    @RequiresPermissions("cert:certificate:renew")
    @OperationLog(module = "证书管理", type = OperationType.UPDATE, description = "续期证书")
    public Result<Void> renew(@PathVariable Long id) {
        certificateService.renew(id);
        return Result.success();
    }

    @PostMapping("/{id}/deploy")
    @RequiresPermissions("cert:certificate:deploy")
    @OperationLog(module = "证书管理", type = OperationType.OTHER, description = "部署证书")
    public Result<String> deploy(@PathVariable Long id) {
        return Result.success(certificateService.deployNow(id));
    }

    @PutMapping("/{id}/bindings")
    @RequiresPermissions("cert:certificate:deploy")
    @OperationLog(module = "证书管理", type = OperationType.UPDATE, description = "更新部署目标绑定")
    public Result<Void> updateBindings(@PathVariable Long id, @RequestBody List<Long> targetIds) {
        certificateService.updateBindings(id, targetIds);
        return Result.success();
    }

    @GetMapping("/{id}/pem")
    @RequiresPermissions("cert:certificate:query")
    public Result<Map<String, String>> exportPem(@PathVariable Long id) {
        return Result.success(certificateService.exportPem(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("cert:certificate:delete")
    @OperationLog(module = "证书管理", type = OperationType.DELETE, description = "删除证书")
    public Result<Void> delete(@PathVariable Long id) {
        certificateService.delete(id);
        return Result.success();
    }
}
