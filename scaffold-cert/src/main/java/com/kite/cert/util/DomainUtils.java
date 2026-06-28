package com.kite.cert.util;

import java.util.Set;

/**
 * 域名工具：从完整域名推导可注册域名(zone)与解析主机记录(host)。
 *
 * <p>采用内置常见二级公共后缀表的简化策略，覆盖 .com/.cn/.com.cn 等常见场景；
 * 如遇特殊后缀可在 {@link #SECOND_LEVEL_SUFFIXES} 中补充。</p>
 */
public final class DomainUtils {

    /** ACME DNS-01 验证固定前缀。 */
    public static final String ACME_PREFIX = "_acme-challenge";

    /** 常见的「二级公共后缀」，命中则可注册域名取最后三段。 */
    private static final Set<String> SECOND_LEVEL_SUFFIXES = Set.of(
            "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn", "ac.cn", "mil.cn",
            "com.hk", "org.hk", "net.hk", "com.tw", "org.tw", "net.tw",
            "co.jp", "co.uk", "org.uk", "co.kr", "com.sg"
    );

    private DomainUtils() {
    }

    /**
     * 去掉通配前缀 {@code *.}。
     */
    public static String stripWildcard(String domain) {
        if (domain == null) {
            return null;
        }
        return domain.startsWith("*.") ? domain.substring(2) : domain;
    }

    /**
     * 推导可注册域名（DNS zone）。例如 www.example.com -> example.com；a.b.example.com.cn -> example.com.cn。
     */
    public static String registrableDomain(String host) {
        String d = stripWildcard(host);
        String[] parts = d.split("\\.");
        if (parts.length <= 2) {
            return d;
        }
        String lastTwo = parts[parts.length - 2] + "." + parts[parts.length - 1];
        if (SECOND_LEVEL_SUFFIXES.contains(lastTwo) && parts.length >= 3) {
            return parts[parts.length - 3] + "." + lastTwo;
        }
        return lastTwo;
    }

    /**
     * 计算 DNS-01 验证的完整记录名，例如 _acme-challenge.www.example.com。
     */
    public static String acmeRecordName(String domain) {
        return ACME_PREFIX + "." + stripWildcard(domain);
    }

    /**
     * 计算记录名相对于 zone 的主机部分，例如 _acme-challenge.www（zone=example.com）。
     */
    public static String hostRelativeTo(String fullRecordName, String baseDomain) {
        String suffix = "." + baseDomain;
        if (fullRecordName.endsWith(suffix)) {
            return fullRecordName.substring(0, fullRecordName.length() - suffix.length());
        }
        return fullRecordName;
    }
}
