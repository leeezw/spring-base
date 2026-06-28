package com.kite.cert.controller;

import com.kite.cert.dto.response.DashboardResponse;
import com.kite.cert.service.CertCertificateService;
import com.kite.common.response.Result;
import com.kite.permission.annotation.RequiresPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 证书概览统计接口。
 */
@RestController
@RequestMapping("/api/cert/dashboard")
@RequiredArgsConstructor
public class CertDashboardController {

    private final CertCertificateService certificateService;

    @GetMapping
    @RequiresPermissions("cert:certificate:query")
    public Result<DashboardResponse> dashboard() {
        return Result.success(certificateService.dashboard());
    }
}
