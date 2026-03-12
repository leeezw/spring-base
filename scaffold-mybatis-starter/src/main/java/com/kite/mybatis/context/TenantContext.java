package com.kite.mybatis.context;

/**
 * 租户上下文
 */
public class TenantContext {
    
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE = new ThreadLocal<>();
    
    /**
     * 设置租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    /**
     * 获取租户ID
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }
    
    /**
     * 设置忽略租户过滤
     */
    public static void setIgnore(boolean ignore) {
        IGNORE.set(ignore);
    }
    
    /**
     * 是否忽略租户过滤
     */
    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }
    
    /**
     * 清除租户ID
     */
    public static void clear() {
        TENANT_ID.remove();
        IGNORE.remove();
    }
}
