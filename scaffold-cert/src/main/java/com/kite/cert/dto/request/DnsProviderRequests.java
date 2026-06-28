package com.kite.cert.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * DNS 服务商请求 DTO。
 */
public class DnsProviderRequests {

    @Data
    public static class Save {
        @NotBlank(message = "名称不能为空")
        private String name;

        /** west/aliyun/dnspod/cloudflare/manual */
        @NotBlank(message = "类型不能为空")
        private String type;

        /** 凭证键值对，如 {username, apiPassword}。 */
        private Map<String, String> credential;

        private Integer status = 1;
        private String remark;
    }

    @Data
    public static class Update extends Save {
        private Long id;
    }
}
