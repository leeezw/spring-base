package com.kite.cert.deploy;

import java.util.Map;

/**
 * 证书部署 SPI：把签发好的证书推送到目标（Nginx/宝塔/七牛云等）。
 *
 * <p>新增目标只需实现本接口并注册为 Spring Bean，{@link DeployerFactory} 会按 {@link #supports(String)} 路由。</p>
 */
public interface CertDeployer {

    /** 是否支持该目标类型。一个实现可支持多种类型（如 SSH 同时覆盖 nginx_ssh/west_ssh）。 */
    boolean supports(String type);

    /** 部署证书。失败请抛异常，编排层会记录失败状态。 */
    void deploy(DeployContext context);

    /** 连通性测试（不部署证书）。 */
    void test(String type, Map<String, String> config);
}
