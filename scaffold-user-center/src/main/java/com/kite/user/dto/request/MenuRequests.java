package com.kite.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class MenuRequests {

    private MenuRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "菜单名称不能为空")
        @Size(max = 50, message = "菜单名称长度不能超过50位")
        private String menuName;

        @Min(value = 0, message = "上级菜单值无效")
        private Long parentId;

        @Pattern(regexp = "^$|" + ValidationPatterns.MENU_PATH, message = "路由路径格式不正确，应以 / 开头")
        private String path;

        @Pattern(regexp = "^$|" + ValidationPatterns.COMPONENT, message = "前端组件格式不正确")
        private String component;

        @Pattern(regexp = "^$|" + ValidationPatterns.ICON, message = "图标格式不正确")
        private String icon;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;

        @NotNull(message = "显示状态不能为空")
        @Min(value = 0, message = "显示状态值无效")
        @Max(value = 1, message = "显示状态值无效")
        private Integer visible;

        @NotNull(message = "启用状态不能为空")
        @Min(value = 0, message = "启用状态值无效")
        @Max(value = 1, message = "启用状态值无效")
        private Integer status;
    }

    @Data
    public static class Update extends Save {
        @NotNull(message = "菜单ID不能为空")
        private Long id;
    }
}
