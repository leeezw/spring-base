package com.kite.cert.dns;

import java.util.Map;

/**
 * DNS 服务商 SPI：用于 DNS-01 验证时自动写入/清理 TXT 记录。
 *
 * <p>新增服务商只需实现本接口并注册为 Spring Bean，{@link DnsProviderFactory} 会按 {@link #type()} 自动路由。</p>
 */
public interface DnsProvider {

    /** 服务商类型标识，对应 cert_dns_provider.type，如 west/aliyun/dnspod/cloudflare/manual。 */
    String type();

    /**
     * 添加 TXT 记录。
     *
     * @param challenge  待写入的记录
     * @param credential 已解密的凭证（键值对）
     */
    void addTxtRecord(DnsChallenge challenge, Map<String, String> credential);

    /**
     * 删除（清理）TXT 记录。验证完成后调用，失败应吞掉异常仅记录日志，不影响主流程。
     */
    void removeTxtRecord(DnsChallenge challenge, Map<String, String> credential);
}
