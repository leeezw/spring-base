package com.kite.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class PostRequests {

    private PostRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "岗位名称不能为空")
        @Size(max = 50, message = "岗位名称长度不能超过50位")
        private String postName;

        @NotBlank(message = "岗位编码不能为空")
        @Pattern(regexp = ValidationPatterns.POST_CODE, message = "岗位编码需以字母开头，只能包含字母、数字和下划线")
        private String postCode;

        @NotNull(message = "岗位类别不能为空")
        @Min(value = 1, message = "岗位类别值无效")
        private Integer postCategory;

        private Long deptId;

        @Size(max = 200, message = "岗位描述长度不能超过200位")
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
        @NotNull(message = "岗位ID不能为空")
        private Long id;
    }
}
