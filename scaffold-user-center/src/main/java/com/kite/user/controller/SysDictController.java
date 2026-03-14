package com.kite.user.controller;

import com.kite.common.response.Result;
import com.kite.user.entity.SysDict;
import com.kite.user.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService sysDictService;

    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) Integer status) {
        return Result.success(sysDictService.page(pageNum, pageSize, dictName, status));
    }

    @GetMapping("/{id}")
    public Result<SysDict> getById(@PathVariable Long id) {
        return Result.success(sysDictService.getById(id));
    }

    @PostMapping
    public Result<?> add(@RequestBody SysDict entity) {
        sysDictService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody SysDict entity) {
        sysDictService.updateById(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysDictService.removeById(id);
        return Result.success();
    }
}
