package com.kite.beauty.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public final class MemberRequests {

    private MemberRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "会员姓名不能为空")
        @Size(max = 64, message = "会员姓名长度不能超过64个字符")
        private String name;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;

        @Min(value = 0, message = "性别值无效")
        @Max(value = 2, message = "性别值无效")
        private Integer gender;

        private LocalDate birthday;

        @Size(max = 32, message = "来源长度不能超过32个字符")
        private String source;

        @Size(max = 32, message = "会员等级长度不能超过32个字符")
        private String level;

        @Size(max = 20, message = "标签数量不能超过20个")
        private List<@Size(max = 32, message = "单个标签长度不能超过32个字符") String> tags;

        @NotNull(message = "归属门店不能为空")
        @Min(value = 1, message = "归属门店无效")
        private Long belongStoreId;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;

        @Size(max = 500, message = "备注长度不能超过500个字符")
        private String remark;
    }

    @Data
    public static class Update extends Save {
        private Long id;
    }

    @Data
    public static class StatusUpdate {
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }
}
