package com.kite.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.common.response.Result;
import com.kite.user.dto.request.GenTableRequests;
import com.kite.user.dto.request.RequestConverters;
import com.kite.user.entity.GenTable;
import com.kite.user.service.GenTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tool/gen")
@RequiredArgsConstructor
public class GenController {

    private final GenTableService genTableService;

    @GetMapping("/db/tables")
    public Result<List<Map<String, Object>>> listDbTables() {
        return Result.success(genTableService.listDbTables());
    }

    @GetMapping("/db/columns")
    public Result<List<Map<String, Object>>> listDbColumns(@RequestParam String tableName) {
        return Result.success(genTableService.listDbColumns(tableName));
    }

    @GetMapping("/list")
    public Result<List<GenTable>> list() {
        List<GenTable> list = genTableService.list(new LambdaQueryWrapper<GenTable>().orderByDesc(GenTable::getCreateTime));
        return Result.success(list);
    }

    @PostMapping("/import")
    public Result<GenTable> importTable(@RequestParam String tableName) {
        return Result.success(genTableService.importTable(tableName));
    }

    @GetMapping("/{id}")
    public Result<GenTable> getDetail(@PathVariable Long id) {
        return Result.success(genTableService.getTableDetail(id));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody GenTableRequests.Update request) {
        genTableService.updateTable(RequestConverters.toGenTable(request));
        return Result.success();
    }

    @GetMapping("/{id}/preview")
    public Result<Map<String, String>> preview(@PathVariable Long id) {
        return Result.success(genTableService.previewCode(id));
    }

    @PostMapping("/{id}/generate")
    public Result<Map<String, String>> generate(@PathVariable Long id) {
        return Result.success(genTableService.generateToProject(id));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        byte[] zip = genTableService.downloadZip(id);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=code.zip");
        response.getOutputStream().write(zip);
        response.getOutputStream().flush();
    }

    @PostMapping("/{id}/sync")
    public Result<Void> syncTable(@PathVariable Long id) {
        genTableService.syncTable(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        genTableService.deleteTable(id);
        return Result.success();
    }
}
