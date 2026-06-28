package com.kite.cert.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 部署目标请求 DTO。
 */
public class DeployTargetRequests {

    @Data
    public static class Save {
        @NotBlank(message = "名称不能为空")
        private String name;

        /** nginx_ssh/west_ssh/baota/qiniu */
        @NotBlank(message = "类型不能为空")
        private String type;

        /** 目标配置键值对（按类型不同），如 SSH:{host,port,username,...}。 */
        private Map<String, String> config;

        private Integer enabled = 1;
        private String remark;
    }

    @Data
    public static class Update extends Save {
        private Long id;
    }
}
