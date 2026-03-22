package com.kite.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class DeptRequests {

    private DeptRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "部门名称不能为空")
        @Size(max = 50, message = "部门名称长度不能超过50个字符")
        private String deptName;

        @Min(value = 0, message = "上级部门值无效")
        private Long parentId;

        @Min(value = 0, message = "负责人ID值无效")
        private Long leaderId;

        @Pattern(regexp = "^$|" + ValidationPatterns.PHONE, message = "手机号或座机格式不正确")
        private String phone;

        @Pattern(regexp = "^$|" + ValidationPatterns.EMAIL, message = "邮箱格式不正确")
        private String email;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }

    @Data
    public static class Update extends Save {
        @NotNull(message = "部门ID不能为空")
        private Long id;
    }
}
