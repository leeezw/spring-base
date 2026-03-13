package com.kite.mybatis.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.kite.mybatis.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Arrays;
import java.util.List;

/**
 * 多租户处理器
 */
public class CustomTenantHandler implements TenantLineHandler {
    
    /**
     * 不需要租户隔离的表
     */
    private static final List<String> IGNORE_TABLES = Arrays.asList(
            "sys_tenant",           // 租户表本身
            "sys_user_role",        // 关联表
            "sys_role_permission",  // 关联表
            "sys_role_menu",        // 关联表
            "sys_user_post",        // 关联表
            "sys_role_dept",         // 关联表（数据权限-自定义部门）
            "gen_table",             // 代码生成配置（无tenant_id）
            "gen_table_column"       // 代码生成列配置（无tenant_id）
    );
    
    /**
     * 获取租户ID
     */
    @Override
    public Expression getTenantId() {
        // 如果设置了忽略标记，返回null表示不过滤
        if (TenantContext.isIgnore()) {
            return null;
        }
        
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // 如果没有设置租户ID，默认使用1（防止查询出所有租户数据）
            tenantId = 1L;
        }
        return new LongValue(tenantId);
    }
    
    /**
     * 获取租户字段名
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
    
    /**
     * 判断表是否需要租户隔离
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 如果设置了忽略标记，所有表都不过滤
        if (TenantContext.isIgnore()) {
            return true;
        }
        return IGNORE_TABLES.contains(tableName);
    }
}
