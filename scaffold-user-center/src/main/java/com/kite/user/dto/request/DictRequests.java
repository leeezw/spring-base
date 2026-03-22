package com.kite.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class DictRequests {

    private DictRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "字典名称不能为空")
        @Size(max = 50, message = "字典名称长度不能超过50位")
        private String dictName;

        @NotBlank(message = "字典编码不能为空")
        @Pattern(regexp = ValidationPatterns.DICT_CODE, message = "字典编码需以小写字母开头，只能包含小写字母、数字和下划线")
        private String dictCode;

        @Size(max = 200, message = "描述长度不能超过200位")
        private String description;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;
    }

    @Data
    public static class Update extends Save {
        @NotNull(message = "字典ID不能为空")
        private Long id;
    }

    @Data
    public static class ItemSave {
        @NotNull(message = "字典ID不能为空")
        private Long dictId;

        @NotBlank(message = "标签名不能为空")
        @Size(max = 50, message = "标签名长度不能超过50位")
        private String itemLabel;

        @NotBlank(message = "数据值不能为空")
        @Size(max = 50, message = "数据值长度不能超过50位")
        private String itemValue;

        @Pattern(regexp = "^$|" + ValidationPatterns.COLOR, message = "标签颜色格式不正确")
        private String itemColor;

        @Pattern(regexp = "^$|" + ValidationPatterns.ICON, message = "图标格式不正确")
        private String itemIcon;

        @Size(max = 200, message = "描述长度不能超过200位")
        private String description;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值无效")
        @Max(value = 1, message = "状态值无效")
        private Integer status;

        @NotNull(message = "默认值标记不能为空")
        @Min(value = 0, message = "默认值标记无效")
        @Max(value = 1, message = "默认值标记无效")
        private Integer isDefault;
    }

    @Data
    public static class ItemUpdate extends ItemSave {
        @NotNull(message = "数据项ID不能为空")
        private Long id;
    }
}
