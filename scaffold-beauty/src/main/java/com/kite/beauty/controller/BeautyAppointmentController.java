package com.kite.beauty.controller;

import com.kite.beauty.dto.request.AppointmentRequests;
import com.kite.beauty.dto.response.BeautyAppointmentResponse;
import com.kite.beauty.service.BeautyAppointmentService;
import com.kite.common.response.PageResult;
import com.kite.common.response.Result;
import com.kite.log.annotation.OperationLog;
import com.kite.log.annotation.OperationLog.OperationType;
import com.kite.permission.annotation.RequiresPermissions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/beauty/appointments")
@RequiredArgsConstructor
public class BeautyAppointmentController {

    private final BeautyAppointmentService appointmentService;

    @GetMapping("/page")
    @RequiresPermissions("beauty:appointment:query")
    public Result<PageResult<BeautyAppointmentResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTo) {
        return Result.success(appointmentService.pageAppointments(pageNum, pageSize, keyword, memberId,
                storeId, employeeId, status, startFrom, startTo));
    }

    @GetMapping("/calendar")
    @RequiresPermissions("beauty:appointment:query")
    public Result<List<BeautyAppointmentResponse>> calendar(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTo) {
        return Result.success(appointmentService.calendarAppointments(storeId, employeeId, startFrom, startTo));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("beauty:appointment:query")
    public Result<BeautyAppointmentResponse> getById(@PathVariable Long id) {
        return Result.success(appointmentService.getAppointmentDetail(id));
    }

    @PostMapping
    @RequiresPermissions("beauty:appointment:add")
    @OperationLog(module = "预约管理", type = OperationType.INSERT, description = "新增预约")
    public Result<Void> add(@Valid @RequestBody AppointmentRequests.Save request) {
        appointmentService.addAppointment(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "编辑预约")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AppointmentRequests.Update request) {
        request.setId(id);
        appointmentService.updateAppointment(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/reschedule")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "预约改期")
    public Result<Void> reschedule(@PathVariable Long id, @Valid @RequestBody AppointmentRequests.Reschedule request) {
        appointmentService.reschedule(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/confirm")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "确认预约")
    public Result<Void> confirm(@PathVariable Long id) {
        appointmentService.confirm(id);
        return Result.success();
    }

    @PutMapping("/{id}/arrive")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "确认到店")
    public Result<Void> arrive(@PathVariable Long id) {
        appointmentService.arrive(id);
        return Result.success();
    }

    @PutMapping("/{id}/start")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "开始服务")
    public Result<Void> start(@PathVariable Long id) {
        appointmentService.start(id);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "完成预约")
    public Result<Void> complete(@PathVariable Long id) {
        appointmentService.complete(id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "取消预约")
    public Result<Void> cancel(@PathVariable Long id,
                               @Valid @RequestBody(required = false) AppointmentRequests.Reason request) {
        appointmentService.cancel(id, request == null ? null : request.getReason());
        return Result.success();
    }

    @PutMapping("/{id}/no-show")
    @RequiresPermissions("beauty:appointment:edit")
    @OperationLog(module = "预约管理", type = OperationType.UPDATE, description = "标记爽约")
    public Result<Void> noShow(@PathVariable Long id,
                               @Valid @RequestBody(required = false) AppointmentRequests.Reason request) {
        appointmentService.noShow(id, request == null ? null : request.getReason());
        return Result.success();
    }
}
