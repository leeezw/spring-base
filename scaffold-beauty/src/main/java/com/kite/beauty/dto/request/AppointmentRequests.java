package com.kite.beauty.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public final class AppointmentRequests {

    private AppointmentRequests() {
    }

    @Data
    public static class Save {
        @NotNull(message = "会员不能为空")
        @Min(value = 1, message = "会员无效")
        private Long memberId;

        @NotNull(message = "门店不能为空")
        @Min(value = 1, message = "门店无效")
        private Long storeId;

        @Min(value = 1, message = "服务项目无效")
        private Long serviceItemId;

        @NotBlank(message = "预约项目不能为空")
        @Size(max = 100, message = "预约项目长度不能超过100个字符")
        private String serviceItemName;

        @Min(value = 1, message = "服务员工无效")
        private Long employeeId;

        @NotNull(message = "预约开始时间不能为空")
        private LocalDateTime startTime;

        @NotNull(message = "预约结束时间不能为空")
        private LocalDateTime endTime;

        @Size(max = 32, message = "预约来源长度不能超过32个字符")
        private String source;

        @Size(max = 500, message = "备注长度不能超过500个字符")
        private String remark;
    }

    @Data
    public static class Update extends Save {
        private Long id;
    }

    @Data
    public static class Reschedule {
        @NotNull(message = "预约开始时间不能为空")
        private LocalDateTime startTime;

        @NotNull(message = "预约结束时间不能为空")
        private LocalDateTime endTime;

        @Size(max = 255, message = "改期原因长度不能超过255个字符")
        private String reason;
    }

    @Data
    public static class Reason {
        @Size(max = 255, message = "原因长度不能超过255个字符")
        private String reason;
    }

    @Data
    public static class StatusUpdate {
        @NotNull(message = "预约状态不能为空")
        @Min(value = 0, message = "预约状态值无效")
        @Max(value = 6, message = "预约状态值无效")
        private Integer status;
    }
}
