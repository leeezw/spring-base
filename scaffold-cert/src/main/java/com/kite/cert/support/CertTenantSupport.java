package com.kite.cert.support;

import com.kite.mybatis.context.TenantContext;

/**
 * 租户上下文辅助：写入时填充当前租户。
 */
public final class CertTenantSupport {

    private static final long DEFAULT_TENANT_ID = 1L;

    private CertTenantSupport() {
    }

    public static Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }
}
