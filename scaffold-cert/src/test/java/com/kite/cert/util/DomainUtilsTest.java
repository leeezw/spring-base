package com.kite.cert.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DomainUtils 单元测试（纯函数，无外部依赖）。
 */
class DomainUtilsTest {

    @Test
    void registrableDomain_basic() {
        assertEquals("example.com", DomainUtils.registrableDomain("example.com"));
        assertEquals("example.com", DomainUtils.registrableDomain("www.example.com"));
        assertEquals("example.com", DomainUtils.registrableDomain("a.b.example.com"));
    }

    @Test
    void registrableDomain_secondLevelSuffix() {
        assertEquals("example.com.cn", DomainUtils.registrableDomain("www.example.com.cn"));
        assertEquals("example.com.cn", DomainUtils.registrableDomain("a.b.example.com.cn"));
    }

    @Test
    void registrableDomain_wildcard() {
        assertEquals("example.com", DomainUtils.registrableDomain("*.example.com"));
    }

    @Test
    void stripWildcard() {
        assertEquals("example.com", DomainUtils.stripWildcard("*.example.com"));
        assertEquals("www.example.com", DomainUtils.stripWildcard("www.example.com"));
    }

    @Test
    void acmeRecordName() {
        assertEquals("_acme-challenge.example.com", DomainUtils.acmeRecordName("example.com"));
        assertEquals("_acme-challenge.www.example.com", DomainUtils.acmeRecordName("www.example.com"));
        // 通配域名验证记录挂在基础域名上
        assertEquals("_acme-challenge.example.com", DomainUtils.acmeRecordName("*.example.com"));
    }

    @Test
    void hostRelativeTo() {
        assertEquals("_acme-challenge",
                DomainUtils.hostRelativeTo("_acme-challenge.example.com", "example.com"));
        assertEquals("_acme-challenge.www",
                DomainUtils.hostRelativeTo("_acme-challenge.www.example.com", "example.com"));
    }
}
