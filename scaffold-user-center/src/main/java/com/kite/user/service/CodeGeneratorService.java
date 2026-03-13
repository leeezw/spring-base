package com.kite.user.service;

import com.kite.user.entity.GenTable;
import com.kite.user.entity.GenTableColumn;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成模板服务
 */
@Service
public class CodeGeneratorService {

    public Map<String, String> generate(GenTable table) {
        Map<String, String> codes = new LinkedHashMap<>();
        List<GenTableColumn> columns = table.getColumns();
        if (columns == null) columns = new ArrayList<>();

        // 过滤基础字段
        List<GenTableColumn> bizColumns = columns.stream()
            .filter(c -> !Set.of("id", "create_time", "update_time", "create_by", "update_by", "deleted", "tenant_id")
                .contains(c.getColumnName()))
            .collect(Collectors.toList());

        List<GenTableColumn> insertColumns = columns.stream().filter(c -> Boolean.TRUE.equals(c.getIsInsert())).collect(Collectors.toList());
        List<GenTableColumn> editColumns = columns.stream().filter(c -> Boolean.TRUE.equals(c.getIsEdit())).collect(Collectors.toList());
        List<GenTableColumn> listColumns = columns.stream().filter(c -> Boolean.TRUE.equals(c.getIsList())).collect(Collectors.toList());
        List<GenTableColumn> queryColumns = columns.stream().filter(c -> Boolean.TRUE.equals(c.getIsQuery())).collect(Collectors.toList());

        String pkg = table.getPackageName();
        String cls = table.getClassName();
        String business = table.getBusinessName();
        String func = table.getFunctionName();

        codes.put("Entity.java", genEntity(pkg, cls, columns));
        codes.put("Mapper.java", genMapper(pkg, cls));
        codes.put("Service.java", genService(pkg, cls, queryColumns));
        codes.put("Controller.java", genController(pkg, cls, business, func, queryColumns));
        codes.put("Page.jsx", genFrontendPage(table, listColumns, insertColumns, queryColumns));
        codes.put("api.js", genFrontendApi(business));
        codes.put("SQL-menu.sql", genMenuSql(business, func));

        return codes;
    }

    private String genEntity(String pkg, String cls, List<GenTableColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".entity;\n\n");
        
        Set<String> imports = new TreeSet<>();
        imports.add("com.baomidou.mybatisplus.annotation.TableName");
        imports.add(pkg + ".entity.BaseEntity"); // 假设有BaseEntity
        
        boolean hasLocalDateTime = columns.stream().anyMatch(c -> "LocalDateTime".equals(c.getJavaType()));
        boolean hasLocalDate = columns.stream().anyMatch(c -> "LocalDate".equals(c.getJavaType()));
        if (hasLocalDateTime) imports.add("java.time.LocalDateTime");
        if (hasLocalDate) imports.add("java.time.LocalDate");
        
        // 用lombok简化
        imports.add("lombok.Data");
        
        for (String imp : imports) sb.append("import ").append(imp).append(";\n");
        sb.append("\n@Data\n@TableName(\"").append(toSnakeCase(cls)).append("\")\n");
        sb.append("public class ").append(cls).append(" extends BaseEntity {\n\n");

        for (GenTableColumn col : columns) {
            if (Set.of("id", "create_time", "update_time", "create_by", "update_by", "deleted", "tenant_id").contains(col.getColumnName())) continue;
            if (col.getColumnComment() != null && !col.getColumnComment().isEmpty()) {
                sb.append("    /** ").append(col.getColumnComment()).append(" */\n");
            }
            sb.append("    private ").append(col.getJavaType()).append(" ").append(col.getJavaField()).append(";\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String genMapper(String pkg, String cls) {
        return "package " + pkg + ".mapper;\n\n" +
               "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
               "import " + pkg + ".entity." + cls + ";\n" +
               "import org.apache.ibatis.annotations.Mapper;\n\n" +
               "@Mapper\n" +
               "public interface " + cls + "Mapper extends BaseMapper<" + cls + "> {\n}\n";
    }

    private String genService(String pkg, String cls, List<GenTableColumn> queryColumns) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".service;\n\n");
        sb.append("import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;\n");
        sb.append("import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n");
        sb.append("import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n");
        sb.append("import ").append(pkg).append(".entity.").append(cls).append(";\n");
        sb.append("import ").append(pkg).append(".mapper.").append(cls).append("Mapper;\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import org.springframework.util.StringUtils;\n\n");
        sb.append("@Service\n");
        sb.append("public class ").append(cls).append("Service extends ServiceImpl<").append(cls).append("Mapper, ").append(cls).append("> {\n\n");
        sb.append("    public Page<").append(cls).append("> page(int pageNum, int pageSize");

        for (GenTableColumn col : queryColumns) {
            sb.append(", ").append(col.getJavaType()).append(" ").append(col.getJavaField());
        }
        sb.append(") {\n");
        sb.append("        LambdaQueryWrapper<").append(cls).append("> wrapper = new LambdaQueryWrapper<>();\n");
        for (GenTableColumn col : queryColumns) {
            String getter = cls + "::get" + Character.toUpperCase(col.getJavaField().charAt(0)) + col.getJavaField().substring(1);
            if ("String".equals(col.getJavaType())) {
                sb.append("        wrapper.like(StringUtils.hasText(").append(col.getJavaField()).append("), ").append(getter).append(", ").append(col.getJavaField()).append(");\n");
            } else {
                sb.append("        wrapper.eq(").append(col.getJavaField()).append(" != null, ").append(getter).append(", ").append(col.getJavaField()).append(");\n");
            }
        }
        sb.append("        wrapper.orderByDesc(").append(cls).append("::getCreateTime);\n");
        sb.append("        return page(new Page<>(pageNum, pageSize), wrapper);\n");
        sb.append("    }\n}\n");
        return sb.toString();
    }

    private String genController(String pkg, String cls, String business, String func, List<GenTableColumn> queryColumns) {
        String svcVar = Character.toLowerCase(cls.charAt(0)) + cls.substring(1) + "Service";
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".controller;\n\n");
        sb.append("import com.kite.common.response.Result;\n");
        sb.append("import ").append(pkg).append(".entity.").append(cls).append(";\n");
        sb.append("import ").append(pkg).append(".service.").append(cls).append("Service;\n");
        sb.append("import lombok.RequiredArgsConstructor;\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n\n");
        sb.append("@RestController\n");
        sb.append("@RequestMapping(\"/api/").append(business).append("\")\n");
        sb.append("@RequiredArgsConstructor\n");
        sb.append("public class ").append(cls).append("Controller {\n\n");
        sb.append("    private final ").append(cls).append("Service ").append(svcVar).append(";\n\n");

        // 分页查询
        sb.append("    @GetMapping(\"/page\")\n");
        sb.append("    public Result<?> page(\n");
        sb.append("            @RequestParam(defaultValue = \"1\") int pageNum,\n");
        sb.append("            @RequestParam(defaultValue = \"10\") int pageSize");
        for (GenTableColumn col : queryColumns) {
            sb.append(",\n            @RequestParam(required = false) ").append(col.getJavaType()).append(" ").append(col.getJavaField());
        }
        sb.append(") {\n");
        sb.append("        return Result.success(").append(svcVar).append(".page(pageNum, pageSize");
        for (GenTableColumn col : queryColumns) sb.append(", ").append(col.getJavaField());
        sb.append("));\n    }\n\n");

        // 详情
        sb.append("    @GetMapping(\"/{id}\")\n");
        sb.append("    public Result<").append(cls).append("> getById(@PathVariable Long id) {\n");
        sb.append("        return Result.success(").append(svcVar).append(".getById(id));\n    }\n\n");

        // 新增
        sb.append("    @PostMapping\n");
        sb.append("    public Result<?> add(@RequestBody ").append(cls).append(" entity) {\n");
        sb.append("        ").append(svcVar).append(".save(entity);\n");
        sb.append("        return Result.success();\n    }\n\n");

        // 修改
        sb.append("    @PutMapping\n");
        sb.append("    public Result<?> update(@RequestBody ").append(cls).append(" entity) {\n");
        sb.append("        ").append(svcVar).append(".updateById(entity);\n");
        sb.append("        return Result.success();\n    }\n\n");

        // 删除
        sb.append("    @DeleteMapping(\"/{id}\")\n");
        sb.append("    public Result<?> delete(@PathVariable Long id) {\n");
        sb.append("        ").append(svcVar).append(".removeById(id);\n");
        sb.append("        return Result.success();\n    }\n}\n");
        return sb.toString();
    }

    private String genFrontendPage(GenTable table, List<GenTableColumn> listColumns, List<GenTableColumn> insertColumns, List<GenTableColumn> queryColumns) {
        String business = table.getBusinessName();
        String func = table.getFunctionName();
        StringBuilder sb = new StringBuilder();
        sb.append("import { useState } from 'react';\n");
        sb.append("import { Button, Modal, Form, Input, InputNumber, Select, Switch, Tag, Space, message, Drawer } from 'antd';\n");
        sb.append("import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';\n");
        sb.append("import ProTableV2 from '../components/ProTableV2.jsx';\n");
        sb.append("import request from '../api/index.js';\n\n");
        sb.append("export default function ").append(Character.toUpperCase(business.charAt(0))).append(business.substring(1)).append("List() {\n");
        sb.append("  const [drawerVisible, setDrawerVisible] = useState(false);\n");
        sb.append("  const [editingRecord, setEditingRecord] = useState(null);\n");
        sb.append("  const [form] = Form.useForm();\n");
        sb.append("  const [refreshKey, setRefreshKey] = useState(0);\n\n");

        // columns
        sb.append("  const columns = [\n");
        for (GenTableColumn col : listColumns) {
            sb.append("    { title: '").append(col.getColumnComment() != null ? col.getColumnComment() : col.getJavaField()).append("', dataIndex: '").append(col.getJavaField()).append("'");
            if ("status".equals(col.getJavaField())) {
                sb.append(", render: (v) => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>");
            }
            sb.append(" },\n");
        }
        sb.append("    {\n      title: '操作', dataIndex: 'action', hideInSearch: true,\n");
        sb.append("      render: (_, record) => (\n        <Space>\n");
        sb.append("          <Button size=\"small\" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>\n");
        sb.append("          <Button size=\"small\" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>删除</Button>\n");
        sb.append("        </Space>\n      ),\n    },\n  ];\n\n");

        // handlers
        sb.append("  const handleEdit = (record) => {\n");
        sb.append("    setEditingRecord(record);\n    form.setFieldsValue(record);\n    setDrawerVisible(true);\n  };\n\n");
        sb.append("  const handleAdd = () => {\n    setEditingRecord(null);\n    form.resetFields();\n    setDrawerVisible(true);\n  };\n\n");
        sb.append("  const handleSubmit = async (values) => {\n");
        sb.append("    try {\n");
        sb.append("      if (editingRecord) {\n        await request.put('/").append(business).append("', { ...values, id: editingRecord.id });\n");
        sb.append("        message.success('更新成功');\n      } else {\n");
        sb.append("        await request.post('/").append(business).append("', values);\n        message.success('创建成功');\n      }\n");
        sb.append("      setDrawerVisible(false);\n      setRefreshKey(k => k + 1);\n");
        sb.append("    } catch (e) { message.error(e.message); }\n  };\n\n");
        sb.append("  const handleDelete = (record) => {\n");
        sb.append("    Modal.confirm({\n      title: '确认删除',\n      content: `确定删除吗？`,\n");
        sb.append("      onOk: async () => {\n        await request.delete(`/").append(business).append("/${record.id}`);\n");
        sb.append("        message.success('删除成功');\n        setRefreshKey(k => k + 1);\n      }\n    });\n  };\n\n");

        // render
        sb.append("  return (\n    <>\n");
        sb.append("      <ProTableV2\n        key={refreshKey}\n        headerTitle=\"").append(func).append("\"\n");
        sb.append("        columns={columns}\n");
        sb.append("        request={(params) => request.get('/").append(business).append("/page', { params })}\n");
        sb.append("        toolBarRender={() => [\n");
        sb.append("          <Button type=\"primary\" icon={<PlusOutlined />} onClick={handleAdd}>新增</Button>\n");
        sb.append("        ]}\n      />\n\n");
        sb.append("      <Drawer\n        title={editingRecord ? '编辑' : '新增'}\n");
        sb.append("        width={480}\n        open={drawerVisible}\n        onClose={() => setDrawerVisible(false)}\n");
        sb.append("        extra={<Space><Button onClick={() => setDrawerVisible(false)}>取消</Button><Button type=\"primary\" onClick={() => form.submit()}>保存</Button></Space>}\n      >\n");
        sb.append("        <Form form={form} layout=\"vertical\" onFinish={handleSubmit}>\n");
        for (GenTableColumn col : insertColumns) {
            String label = col.getColumnComment() != null ? col.getColumnComment() : col.getJavaField();
            sb.append("          <Form.Item name=\"").append(col.getJavaField()).append("\" label=\"").append(label).append("\"");
            if (Boolean.TRUE.equals(col.getIsRequired())) sb.append(" rules={[{ required: true }]}");
            sb.append(">\n");
            switch (col.getHtmlType() != null ? col.getHtmlType() : "input") {
                case "textarea" -> sb.append("            <Input.TextArea rows={3} />\n");
                case "inputNumber" -> sb.append("            <InputNumber style={{ width: '100%' }} />\n");
                case "select" -> sb.append("            <Select options={[{value:1,label:'启用'},{value:0,label:'禁用'}]} />\n");
                case "switch" -> sb.append("            <Switch />\n");
                default -> sb.append("            <Input />\n");
            }
            sb.append("          </Form.Item>\n");
        }
        sb.append("        </Form>\n      </Drawer>\n    </>\n  );\n}\n");
        return sb.toString();
    }

    private String genFrontendApi(String business) {
        return "// " + business + " API\n" +
               "import request from './index.js';\n\n" +
               "export const page" + Character.toUpperCase(business.charAt(0)) + business.substring(1) + " = (params) => request.get('/" + business + "/page', { params });\n" +
               "export const get" + Character.toUpperCase(business.charAt(0)) + business.substring(1) + " = (id) => request.get(`/" + business + "/${id}`);\n" +
               "export const create" + Character.toUpperCase(business.charAt(0)) + business.substring(1) + " = (data) => request.post('/" + business + "', data);\n" +
               "export const update" + Character.toUpperCase(business.charAt(0)) + business.substring(1) + " = (data) => request.put('/" + business + "', data);\n" +
               "export const delete" + Character.toUpperCase(business.charAt(0)) + business.substring(1) + " = (id) => request.delete(`/" + business + "/${id}`);\n";
    }

    private String genMenuSql(String business, String func) {
        return "-- 菜单SQL\n" +
               "INSERT INTO sys_menu (menu_name, menu_type, path, component, parent_id, sort_order, status, icon, tenant_id)\n" +
               "VALUES ('" + func + "', 1, '/" + business + "', '" + Character.toUpperCase(business.charAt(0)) + business.substring(1) + "List', 0, 10, 1, 'AppstoreOutlined', 1);\n";
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
