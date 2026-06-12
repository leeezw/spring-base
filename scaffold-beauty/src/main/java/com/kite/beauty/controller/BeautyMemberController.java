package com.kite.beauty.controller;

import com.kite.beauty.dto.request.FollowRecordRequests;
import com.kite.beauty.dto.request.MemberRequests;
import com.kite.beauty.dto.response.BeautyAppointmentResponse;
import com.kite.beauty.dto.response.BeautyMemberResponse;
import com.kite.beauty.dto.response.BeautyMemberFollowRecordResponse;
import com.kite.beauty.service.BeautyMemberService;
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

@RestController
@RequestMapping("/api/beauty/members")
@RequiredArgsConstructor
public class BeautyMemberController {

    private final BeautyMemberService memberService;

    @GetMapping("/page")
    @RequiresPermissions("beauty:member:query")
    public Result<PageResult<BeautyMemberResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long belongStoreId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer status) {
        return Result.success(memberService.pageMembers(pageNum, pageSize, keyword, belongStoreId, level, status));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("beauty:member:query")
    public Result<BeautyMemberResponse> getById(@PathVariable Long id) {
        return Result.success(memberService.getMemberDetail(id));
    }

    @GetMapping("/{id}/appointments")
    @RequiresPermissions("beauty:member:query")
    public Result<List<BeautyAppointmentResponse>> listAppointments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(memberService.listAppointments(id, limit));
    }

    @GetMapping("/{id}/follow-records")
    @RequiresPermissions("beauty:member:query")
    public Result<List<BeautyMemberFollowRecordResponse>> listFollowRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(memberService.listFollowRecords(id, limit));
    }

    @PostMapping
    @RequiresPermissions("beauty:member:add")
    @OperationLog(module = "会员管理", type = OperationType.INSERT, description = "新增会员")
    public Result<Void> add(@Valid @RequestBody MemberRequests.Save request) {
        memberService.addMember(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("beauty:member:edit")
    @OperationLog(module = "会员管理", type = OperationType.UPDATE, description = "编辑会员")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MemberRequests.Update request) {
        request.setId(id);
        memberService.updateMember(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequiresPermissions("beauty:member:edit")
    @OperationLog(module = "会员管理", type = OperationType.UPDATE, description = "修改会员状态")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody MemberRequests.StatusUpdate request) {
        memberService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @PostMapping("/{id}/follow-records")
    @RequiresPermissions("beauty:member:edit")
    @OperationLog(module = "会员管理", type = OperationType.INSERT, description = "新增会员跟进记录")
    public Result<Void> addFollowRecord(@PathVariable Long id, @Valid @RequestBody FollowRecordRequests.Save request) {
        memberService.addFollowRecord(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("beauty:member:delete")
    @OperationLog(module = "会员管理", type = OperationType.DELETE, description = "删除会员")
    public Result<Void> delete(@PathVariable Long id) {
        memberService.deleteMember(id);
        return Result.success();
    }
}
