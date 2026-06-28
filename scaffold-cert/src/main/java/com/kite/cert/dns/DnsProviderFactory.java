package com.kite.cert.dns;

import com.kite.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按类型路由到对应的 {@link DnsProvider} 实现。
 */
@Component
@RequiredArgsConstructor
public class DnsProviderFactory {

    private final List<DnsProvider> providers;

    private Map<String, DnsProvider> registry;

    public DnsProvider get(String type) {
        if (registry == null) {
            registry = providers.stream().collect(Collectors.toMap(p -> p.type().toLowerCase(), p -> p));
        }
        DnsProvider provider = registry.get(type == null ? null : type.toLowerCase());
        if (provider == null) {
            throw new BusinessException("不支持的 DNS 服务商类型：" + type);
        }
        return provider;
    }
}
