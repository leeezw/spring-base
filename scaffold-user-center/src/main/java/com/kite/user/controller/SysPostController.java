package com.kite.user.controller;

import com.kite.common.response.Result;
import com.kite.user.dto.request.PostRequests;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.entity.SysPost;
import com.kite.user.entity.SysPosition;
import com.kite.user.service.SysPostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/post")
public class SysPostController {

    private final SysPostService postService;

    public SysPostController(SysPostService postService) {
        this.postService = postService;
    }

    @GetMapping("/page")
    public Result<Object> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status) {
        return Result.success(postService.page(page, size, keyword, deptId, status));
    }

    @GetMapping("/list")
    public Result<List<SysPost>> list(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status) {
        return Result.success(postService.list(deptId, status));
    }

    @GetMapping("/dept-tree")
    public Result<List<Map<String, Object>>> deptPostTree() {
        return Result.success(postService.deptPostTree());
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody PostRequests.Save request) {
        postService.create(RequestConverters.toPost(request));
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody PostRequests.Update request) {
        postService.update(RequestConverters.toPost(request));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/positions")
    public Result<List<SysPosition>> getPositions(@PathVariable Long id) {
        return Result.success(postService.getPositionsByPostId(id));
    }
}
