package com.kite.user.controller;

import com.kite.common.response.Result;
import com.kite.user.entity.SysPosition;
import com.kite.user.service.SysPositionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/position")
public class SysPositionController {

    private final SysPositionService positionService;

    public SysPositionController(SysPositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping("/list")
    public Result<List<SysPosition>> list(
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Integer status) {
        return Result.success(positionService.list(postId, status));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysPosition position) {
        positionService.create(position);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysPosition position) {
        positionService.update(position);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return Result.success();
    }
}
