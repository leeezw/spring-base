package com.kite.cert.deploy;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 部署上下文：携带证书材料与目标配置。
 */
@Data
@Builder
public class DeployContext {

    /** 部署目标类型，如 nginx_ssh/west_ssh/baota/qiniu。 */
    private String type;

    private String primaryDomain;

    /** 叶子证书 PEM。 */
    private String certPem;

    /** 完整证书链 PEM（fullchain）。 */
    private String chainPem;

    /** 私钥 PEM。 */
    private String keyPem;

    /** 已解密的目标配置。 */
    private Map<String, String> config;
}
