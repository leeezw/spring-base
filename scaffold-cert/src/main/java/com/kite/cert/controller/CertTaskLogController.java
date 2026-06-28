package com.kite.cert.controller;

import com.kite.cert.dto.response.TaskLogResponse;
import com.kite.cert.service.CertTaskLogService;
import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.permission.annotation.RequiresPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 证书任务日志接口。
 */
@RestController
@RequestMapping("/api/cert/task-logs")
@RequiredArgsConstructor
public class CertTaskLogController {

    private final CertTaskLogService taskLogService;

    @GetMapping("/page")
    @RequiresPermissions("cert:log:query")
    public Result<PageResult<TaskLogResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long certificateId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return Result.success(taskLogService.page(pageNum, pageSize, certificateId, type, status));
    }
}
