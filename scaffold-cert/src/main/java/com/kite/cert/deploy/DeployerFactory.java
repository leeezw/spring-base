package com.kite.cert.deploy;

import com.kite.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按类型路由到对应的 {@link CertDeployer} 实现。
 */
@Component
@RequiredArgsConstructor
public class DeployerFactory {

    private final List<CertDeployer> deployers;

    public CertDeployer get(String type) {
        return deployers.stream()
                .filter(d -> d.supports(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException("不支持的部署目标类型：" + type));
    }
}
