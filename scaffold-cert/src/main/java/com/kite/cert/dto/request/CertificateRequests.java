package com.kite.cert.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 证书签发/续期请求 DTO。
 */
public class CertificateRequests {

    @Data
    public static class Issue {
        /** 域名列表，第一个为主域名，支持通配 *.example.com。 */
        @NotEmpty(message = "域名不能为空")
        private List<String> domains;

        @NotNull(message = "请选择 ACME 账户")
        private Long acmeAccountId;

        /** DNS-01 验证所用的 DNS 服务商。 */
        @NotNull(message = "请选择 DNS 服务商")
        private Long dnsProviderId;

        /** RSA2048 / EC256，默认 RSA2048。 */
        private String keyAlgo = "RSA2048";

        private Integer autoRenew = 1;
        private Integer renewBeforeDays = 30;

        /** 绑定的部署目标 ID 列表（签发成功后自动部署）。 */
        private List<Long> deployTargetIds;
    }
}
