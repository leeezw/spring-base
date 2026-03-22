package com.kite.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public final class TenantRequests {

    private TenantRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "租户编码不能为空")
        @Pattern(regexp = ValidationPatterns.TENANT_CODE, message = "租户编码需以字母开头，只能包含字母、数字和下划线")
        private String tenantCode;

        @NotBlank(message = "租户名称不能为空")
        @Size(max = 50, message = "租户名称长度不能超过50个字符")
        private String tenantName;

        @Size(max = 50, message = "联系人长度不能超过50个字符")
        private String contactName;

        @Pattern(regexp = "^$|" + ValidationPatterns.PHONE, message = "手机号或座机格式不正确")
        private String contactPhone;

        @Pattern(regexp = "^$|" + ValidationPatterns.EMAIL, message = "邮箱格式不正确")
        private String contactEmail;

        private LocalDateTime expireTime;

        private Integer accountCount;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;

        @Size(max = 255, message = "Logo地址长度不能超过255个字符")
        private String logo;

        @AssertTrue(message = "账号额度只能为-1或大于0")
        public boolean isAccountCountValid() {
            return accountCount == null || accountCount == -1 || accountCount > 0;
        }
    }

    @Data
    public static class Update extends Save {
        @NotNull(message = "租户ID不能为空")
        private Long id;
    }

    @Data
    public static class UpdateStatus {
        @NotNull(message = "租户ID不能为空")
        private Long id;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }
}
