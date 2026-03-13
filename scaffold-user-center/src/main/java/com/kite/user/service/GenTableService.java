package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.user.entity.GenTable;
import com.kite.user.entity.GenTableColumn;
import com.kite.user.mapper.GenTableColumnMapper;
import com.kite.user.mapper.GenTableMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenTableService extends ServiceImpl<GenTableMapper, GenTable> {

    private final GenTableColumnMapper columnMapper;
    private final JdbcTemplate jdbcTemplate;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * 查询数据库所有表
     */
    public List<Map<String, Object>> listDbTables() {
        String sql = "SELECT table_name, obj_description(c.oid) as table_comment " +
                "FROM information_schema.tables t " +
                "LEFT JOIN pg_class c ON c.relname = t.table_name " +
                "WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE' " +
                "ORDER BY t.table_name";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 查询表的列信息
     */
    public List<Map<String, Object>> listDbColumns(String tableName) {
        String sql = "SELECT c.column_name, c.data_type, c.udt_name, c.is_nullable, " +
                "c.column_default, c.character_maximum_length, " +
                "col_description((SELECT oid FROM pg_class WHERE relname = ?), c.ordinal_position) as column_comment, " +
                "CASE WHEN pk.column_name IS NOT NULL THEN true ELSE false END as is_pk " +
                "FROM information_schema.columns c " +
                "LEFT JOIN (SELECT kcu.column_name FROM information_schema.table_constraints tc " +
                "  JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name " +
                "  WHERE tc.table_name = ? AND tc.constraint_type = 'PRIMARY KEY') pk ON c.column_name = pk.column_name " +
                "WHERE c.table_schema = 'public' AND c.table_name = ? " +
                "ORDER BY c.ordinal_position";
        return jdbcTemplate.queryForList(sql, tableName, tableName, tableName);
    }

    /**
     * 导入表
     */
    public GenTable importTable(String tableName) {
        // 检查是否已导入
        GenTable existing = getOne(new LambdaQueryWrapper<GenTable>().eq(GenTable::getTableName, tableName));
        if (existing != null) throw new BusinessException("表已导入，请先删除再重新导入");

        List<Map<String, Object>> dbColumns = listDbColumns(tableName);
        if (dbColumns.isEmpty()) throw new BusinessException("表不存在或没有列");

        // 创建GenTable
        GenTable genTable = new GenTable();
        genTable.setTableName(tableName);
        genTable.setClassName(toPascalCase(tableName));
        genTable.setPackageName("com.kite.user");
        genTable.setModuleName("user");
        genTable.setBusinessName(toCamelCase(tableName.replaceFirst("^(sys_|gen_|biz_)", "")));
        genTable.setFunctionName(tableName);
        genTable.setAuthor("generator");
        genTable.setGenType("zip");
        genTable.setCreateTime(LocalDateTime.now());
        genTable.setUpdateTime(LocalDateTime.now());

        // 查表注释
        try {
            String comment = jdbcTemplate.queryForObject(
                "SELECT obj_description(c.oid) FROM pg_class c WHERE c.relname = ?",
                String.class, tableName);
            genTable.setTableComment(comment);
            if (comment != null && !comment.isEmpty()) genTable.setFunctionName(comment);
        } catch (Exception e) { /* ignore */ }

        save(genTable);

        // 创建列配置
        int sort = 0;
        for (Map<String, Object> col : dbColumns) {
            GenTableColumn column = new GenTableColumn();
            column.setTableId(genTable.getId());
            column.setColumnName((String) col.get("column_name"));
            column.setColumnComment((String) col.get("column_comment"));
            column.setColumnType((String) col.get("udt_name"));
            column.setJavaType(mapJavaType((String) col.get("udt_name")));
            column.setJavaField(toCamelCase((String) col.get("column_name")));
            column.setIsPk(Boolean.TRUE.equals(col.get("is_pk")));
            column.setIsIncrement(column.getIsPk()); // PK默认自增
            column.setSortOrder(sort++);

            // 默认设置
            String colName = column.getColumnName();
            boolean isBaseField = Set.of("id", "create_time", "update_time", "create_by", "update_by", "deleted", "tenant_id")
                    .contains(colName);
            column.setIsRequired(!"YES".equals(col.get("is_nullable")) && !column.getIsPk());
            column.setIsInsert(!isBaseField && !column.getIsPk());
            column.setIsEdit(!isBaseField && !column.getIsPk());
            column.setIsList(!Set.of("deleted", "tenant_id", "create_by", "update_by").contains(colName));
            column.setIsQuery(Set.of("status", "name", "type").stream().anyMatch(colName::contains));
            column.setQueryType("EQ");
            column.setHtmlType(guessHtmlType(column));
            column.setCreateTime(LocalDateTime.now());

            columnMapper.insert(column);
        }

        genTable.setColumns(columnMapper.selectList(
            new LambdaQueryWrapper<GenTableColumn>().eq(GenTableColumn::getTableId, genTable.getId())
                .orderByAsc(GenTableColumn::getSortOrder)));
        return genTable;
    }

    /**
     * 获取表详细信息（含列）
     */
    public GenTable getTableDetail(Long id) {
        GenTable table = getById(id);
        if (table == null) throw new BusinessException("配置不存在");
        table.setColumns(columnMapper.selectList(
            new LambdaQueryWrapper<GenTableColumn>().eq(GenTableColumn::getTableId, id)
                .orderByAsc(GenTableColumn::getSortOrder)));
        return table;
    }

    /**
     * 更新表配置
     */
    public void updateTable(GenTable table) {
        table.setUpdateTime(LocalDateTime.now());
        updateById(table);
        if (table.getColumns() != null) {
            for (GenTableColumn col : table.getColumns()) {
                if (col.getId() != null) {
                    columnMapper.updateById(col);
                }
            }
        }
    }

    /**
     * 预览代码
     */
    public Map<String, String> previewCode(Long tableId) {
        GenTable table = getTableDetail(tableId);
        return codeGeneratorService.generate(table);
    }

    /**
     * 删除表配置
     */
    public void deleteTable(Long id) {
        columnMapper.delete(new LambdaQueryWrapper<GenTableColumn>().eq(GenTableColumn::getTableId, id));
        removeById(id);
    }

    // ========== 工具方法 ==========

    private String toPascalCase(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.split("_")) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private String toCamelCase(String name) {
        String pascal = toPascalCase(name);
        return pascal.isEmpty() ? pascal : Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    private String mapJavaType(String pgType) {
        return switch (pgType) {
            case "int2", "int4" -> "Integer";
            case "int8", "bigserial" -> "Long";
            case "float4" -> "Float";
            case "float8", "numeric" -> "Double";
            case "bool" -> "Boolean";
            case "timestamp", "timestamptz" -> "LocalDateTime";
            case "date" -> "LocalDate";
            case "text", "varchar", "bpchar" -> "String";
            default -> "String";
        };
    }

    private String guessHtmlType(GenTableColumn col) {
        if ("text".equals(col.getColumnType())) return "textarea";
        if ("bool".equals(col.getColumnType())) return "switch";
        if (col.getColumnName().contains("status") || col.getColumnName().contains("type")) return "select";
        if (col.getColumnName().contains("time") || col.getColumnName().contains("date")) return "datetime";
        if ("Long".equals(col.getJavaType()) || "Integer".equals(col.getJavaType())) return "inputNumber";
        return "input";
    }
}
