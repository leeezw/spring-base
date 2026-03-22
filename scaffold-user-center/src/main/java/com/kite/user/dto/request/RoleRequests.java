package com.kite.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class RoleRequests {

    private RoleRequests() {
    }

    @Data
    public static class Create {
        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = ValidationPatterns.ROLE_CODE, message = "角色编码需以字母开头，只能包含字母、数字和下划线")
        private String roleCode;

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称长度不能超过50个字符")
        private String roleName;

        @Size(max = 200, message = "角色描述长度不能超过200个字符")
        private String description;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;

        @NotNull(message = "数据范围不能为空")
        @Min(value = 1, message = "数据范围值无效")
        @Max(value = 5, message = "数据范围值无效")
        private Integer dataScope;

        private List<Long> permissionIds;

        private List<Long> customDeptIds;

        @AssertTrue(message = "自定义数据范围至少选择一个部门")
        public boolean isCustomDeptIdsValid() {
            return dataScope == null || dataScope != 5 || (customDeptIds != null && !customDeptIds.isEmpty());
        }
    }

    @Data
    public static class Update {
        @NotNull(message = "角色ID不能为空")
        private Long id;

        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = ValidationPatterns.ROLE_CODE, message = "角色编码需以字母开头，只能包含字母、数字和下划线")
        private String roleCode;

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称长度不能超过50个字符")
        private String roleName;

        @Size(max = 200, message = "角色描述长度不能超过200个字符")
        private String description;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;

        @NotNull(message = "数据范围不能为空")
        @Min(value = 1, message = "数据范围值无效")
        @Max(value = 5, message = "数据范围值无效")
        private Integer dataScope;

        private List<Long> permissionIds;

        private List<Long> customDeptIds;

        @AssertTrue(message = "自定义数据范围至少选择一个部门")
        public boolean isCustomDeptIdsValid() {
            return dataScope == null || dataScope != 5 || (customDeptIds != null && !customDeptIds.isEmpty());
        }
    }

    @Data
    public static class UpdateStatus {
        @NotNull(message = "角色ID不能为空")
        private Long id;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }
}
