package com.kite.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class PermissionRequests {

    private PermissionRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "权限编码不能为空")
        @Pattern(regexp = ValidationPatterns.PERMISSION_CODE, message = "权限编码格式不正确，例如 system:user:query")
        private String permissionCode;

        @NotBlank(message = "权限名称不能为空")
        @Size(max = 50, message = "权限名称长度不能超过50位")
        private String permissionName;

        @NotNull(message = "权限类型不能为空")
        @Min(value = 1, message = "权限类型值无效")
        @Max(value = 3, message = "权限类型值无效")
        private Integer permissionType;

        @Min(value = 0, message = "上级权限值无效")
        private Long parentId;

        @Pattern(regexp = "^$|" + ValidationPatterns.MENU_PATH, message = "路径格式不正确，应以 / 开头")
        private String path;

        @Pattern(regexp = "^$|" + ValidationPatterns.COMPONENT, message = "前端组件格式不正确")
        private String component;

        @Pattern(regexp = "^$|" + ValidationPatterns.ICON, message = "图标格式不正确")
        private String icon;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }

    @Data
    public static class Update extends Save {
        @NotNull(message = "权限ID不能为空")
        private Long id;
    }
}
