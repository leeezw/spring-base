package com.kite.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class UserRequests {

    private UserRequests() {
    }

    @Data
    public static class Create {
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = ValidationPatterns.USERNAME, message = "用户名需以字母开头，长度为3-20位，只能包含字母、数字和下划线")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度需为6-50位")
        private String password;

        @Size(max = 50, message = "昵称长度不能超过50个字符")
        private String nickname;

        @Pattern(regexp = "^$|" + ValidationPatterns.EMAIL, message = "邮箱格式不正确")
        private String email;

        @Pattern(regexp = "^$|" + ValidationPatterns.PHONE, message = "手机号或座机格式不正确")
        private String phone;

        private Long deptId;

        private List<Long> roleIds;

        private List<Long> postIds;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }

    @Data
    public static class Update {
        @NotNull(message = "用户ID不能为空")
        private Long id;

        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = ValidationPatterns.USERNAME, message = "用户名需以字母开头，长度为3-20位，只能包含字母、数字和下划线")
        private String username;

        @Size(min = 6, max = 50, message = "密码长度需为6-50位")
        private String password;

        @Size(max = 50, message = "昵称长度不能超过50个字符")
        private String nickname;

        @Pattern(regexp = "^$|" + ValidationPatterns.EMAIL, message = "邮箱格式不正确")
        private String email;

        @Pattern(regexp = "^$|" + ValidationPatterns.PHONE, message = "手机号或座机格式不正确")
        private String phone;

        private Long deptId;

        private List<Long> roleIds;

        private List<Long> postIds;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }

    @Data
    public static class UpdateStatus {
        @NotNull(message = "用户ID不能为空")
        private Long id;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }
}
