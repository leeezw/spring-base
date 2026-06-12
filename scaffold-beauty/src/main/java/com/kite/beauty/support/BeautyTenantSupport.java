package com.kite.beauty.support;

import com.kite.mybatis.context.TenantContext;

public final class BeautyTenantSupport {

    private static final long DEFAULT_TENANT_ID = 1L;

    private BeautyTenantSupport() {
    }

    public static Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }
}
