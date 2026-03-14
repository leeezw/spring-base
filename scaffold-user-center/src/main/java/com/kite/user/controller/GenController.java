package com.kite.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.common.response.Result;
import com.kite.user.entity.GenTable;
import com.kite.user.service.GenTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tool/gen")
@RequiredArgsConstructor
public class GenController {

    private final GenTableService genTableService;

    /**
     * 查询数据库表列表
     */
    @GetMapping("/db/tables")
    public Result<List<Map<String, Object>>> listDbTables() {
        return Result.success(genTableService.listDbTables());
    }

    /**
     * 查询表的列信息
     */
    @GetMapping("/db/columns")
    public Result<List<Map<String, Object>>> listDbColumns(@RequestParam String tableName) {
        return Result.success(genTableService.listDbColumns(tableName));
    }

    /**
     * 已导入的表列表
     */
    @GetMapping("/list")
    public Result<List<GenTable>> list() {
        List<GenTable> list = genTableService.list(
            new LambdaQueryWrapper<GenTable>().orderByDesc(GenTable::getCreateTime));
        return Result.success(list);
    }

    /**
     * 导入表
     */
    @PostMapping("/import")
    public Result<GenTable> importTable(@RequestParam String tableName) {
        return Result.success(genTableService.importTable(tableName));
    }

    /**
     * 获取表详细配置（含列）
     */
    @GetMapping("/{id}")
    public Result<GenTable> getDetail(@PathVariable Long id) {
        return Result.success(genTableService.getTableDetail(id));
    }

    /**
     * 更新表配置
     */
    @PutMapping
    public Result<Void> update(@RequestBody GenTable table) {
        genTableService.updateTable(table);
        return Result.success();
    }

    /**
     * 预览代码
     */
    @GetMapping("/{id}/preview")
    public Result<Map<String, String>> preview(@PathVariable Long id) {
        return Result.success(genTableService.previewCode(id));
    }

    /**
     * 生成代码到项目
     */
    @PostMapping("/{id}/generate")
    public Result<Map<String, String>> generate(@PathVariable Long id) {
        return Result.success(genTableService.generateToProject(id));
    }

    /**
     * 下载ZIP
     */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        byte[] zip = genTableService.downloadZip(id);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=code.zip");
        response.getOutputStream().write(zip);
        response.getOutputStream().flush();
    }

    /**
     * 同步表结构（重新读取数据库列信息）
     */
    @PostMapping("/{id}/sync")
    public Result<Void> syncTable(@PathVariable Long id) {
        genTableService.syncTable(id);
        return Result.success();
    }

    /**
     * 删除表配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        genTableService.deleteTable(id);
        return Result.success();
    }
}
