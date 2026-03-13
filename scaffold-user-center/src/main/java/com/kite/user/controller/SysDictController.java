package com.kite.user.controller;

import com.kite.common.response.Result;
import com.kite.user.entity.SysDict;
import com.kite.user.entity.SysDictItem;
import com.kite.user.service.SysDictService;
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

    // ============ 字典类型 ============

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
    public Result<Void> create(@RequestBody SysDict dict) {
        dictService.createDict(dict);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysDict dict) {
        dictService.updateDict(dict);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.deleteDict(id);
        return Result.success();
    }

    // ============ 字典数据 ============

    /** 根据字典编码获取数据项（最常用） */
    @GetMapping("/items/{dictCode}")
    public Result<List<SysDictItem>> getItemsByCode(@PathVariable String dictCode) {
        return Result.success(dictService.getItemsByCode(dictCode));
    }

    /** 批量获取多个字典 */
    @GetMapping("/items/batch")
    public Result<Map<String, List<SysDictItem>>> getItemsBatch(@RequestParam List<String> codes) {
        return Result.success(dictService.getItemsByCodes(codes));
    }

    /** 字典下的数据项列表 */
    @GetMapping("/{dictId}/items")
    public Result<List<SysDictItem>> listItems(@PathVariable Long dictId) {
        return Result.success(dictService.listItemsByDictId(dictId));
    }

    @PostMapping("/item")
    public Result<Void> createItem(@RequestBody SysDictItem item) {
        dictService.createItem(item);
        return Result.success();
    }

    @PutMapping("/item")
    public Result<Void> updateItem(@RequestBody SysDictItem item) {
        dictService.updateItem(item);
        return Result.success();
    }

    @DeleteMapping("/item/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.success();
    }
}
