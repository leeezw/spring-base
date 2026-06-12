package com.kite.beauty.controller;

import com.kite.beauty.dto.request.StoreRequests;
import com.kite.beauty.dto.response.BeautyStoreResponse;
import com.kite.beauty.service.BeautyStoreService;
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
@RequestMapping("/api/beauty/stores")
@RequiredArgsConstructor
public class BeautyStoreController {

    private final BeautyStoreService storeService;

    @GetMapping("/page")
    @RequiresPermissions("beauty:store:query")
    public Result<PageResult<BeautyStoreResponse>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(storeService.pageStores(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/list")
    @RequiresPermissions("beauty:store:query")
    public Result<List<BeautyStoreResponse>> list(@RequestParam(defaultValue = "true") Boolean activeOnly) {
        return Result.success(storeService.listStores(activeOnly));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("beauty:store:query")
    public Result<BeautyStoreResponse> getById(@PathVariable Long id) {
        return Result.success(storeService.getStoreDetail(id));
    }

    @PostMapping
    @RequiresPermissions("beauty:store:add")
    @OperationLog(module = "门店管理", type = OperationType.INSERT, description = "新增门店")
    public Result<Void> add(@Valid @RequestBody StoreRequests.Save request) {
        storeService.addStore(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("beauty:store:edit")
    @OperationLog(module = "门店管理", type = OperationType.UPDATE, description = "编辑门店")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StoreRequests.Update request) {
        request.setId(id);
        storeService.updateStore(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequiresPermissions("beauty:store:edit")
    @OperationLog(module = "门店管理", type = OperationType.UPDATE, description = "修改门店状态")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StoreRequests.StatusUpdate request) {
        storeService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("beauty:store:delete")
    @OperationLog(module = "门店管理", type = OperationType.DELETE, description = "删除门店")
    public Result<Void> delete(@PathVariable Long id) {
        storeService.deleteStore(id);
        return Result.success();
    }
}
