package com.kite.user.controller;

import com.kite.common.response.Result;
import com.kite.user.dto.request.DictRequests;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.entity.SysDict;
import com.kite.user.entity.SysDictItem;
import com.kite.user.service.SysDictService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/dict")
public class SysDictController {

    private final SysDictService dictService;

    public SysDictController(SysDictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/page")
    public Result<Object> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(dictService.pageDicts(page, size, keyword));
    }

    @GetMapping("/list")
    public Result<List<SysDict>> list() {
        return Result.success(dictService.listAll());
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody DictRequests.Save request) {
        dictService.createDict(RequestConverters.toDict(request));
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody DictRequests.Update request) {
        dictService.updateDict(RequestConverters.toDict(request));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.deleteDict(id);
        return Result.success();
    }

    @GetMapping("/items/{dictCode}")
    public Result<List<SysDictItem>> getItemsByCode(@PathVariable String dictCode) {
        return Result.success(dictService.getItemsByCode(dictCode));
    }

    @GetMapping("/items/batch")
    public Result<Map<String, List<SysDictItem>>> getItemsBatch(@RequestParam List<String> codes) {
        return Result.success(dictService.getItemsByCodes(codes));
    }

    @GetMapping("/{dictId}/items")
    public Result<List<SysDictItem>> listItems(@PathVariable Long dictId) {
        return Result.success(dictService.listItemsByDictId(dictId));
    }

    @PostMapping("/item")
    public Result<Void> createItem(@Valid @RequestBody DictRequests.ItemSave request) {
        dictService.createItem(RequestConverters.toDictItem(request));
        return Result.success();
    }

    @PutMapping("/item")
    public Result<Void> updateItem(@Valid @RequestBody DictRequests.ItemUpdate request) {
        dictService.updateItem(RequestConverters.toDictItem(request));
        return Result.success();
    }

    @DeleteMapping("/item/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.success();
    }
}
