package com.kite.cert.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ACME 账户请求 DTO。
 */
public class AcmeAccountRequests {

    @Data
    public static class Save {
        @NotBlank(message = "名称不能为空")
        private String name;

        /** letsencrypt / zerossl */
        @NotBlank(message = "CA 类型不能为空")
        private String caType;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        /** 是否使用测试目录（staging），仅 letsencrypt 有效。 */
        private Boolean staging = false;

        /** 可选：直接指定 directory URL，优先级高于 caType+staging。 */
        private String directoryUrl;

        /** ZeroSSL EAB 凭证。 */
        private String eabKid;
        private String eabHmac;

        private Integer status = 1;
        private String remark;
    }

    @Data
    public static class Update extends Save {
        private Long id;
    }
}
