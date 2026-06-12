package com.kite.beauty.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public final class FollowRecordRequests {

    private FollowRecordRequests() {
    }

    @Data
    public static class Save {
        @Min(value = 1, message = "跟进门店无效")
        private Long storeId;

        @Size(max = 32, message = "跟进方式长度不能超过32个字符")
        private String followType;

        @Size(max = 32, message = "跟进结果长度不能超过32个字符")
        private String followResult;

        @NotBlank(message = "跟进内容不能为空")
        @Size(max = 1000, message = "跟进内容长度不能超过1000个字符")
        private String content;

        private LocalDateTime nextFollowTime;
    }
}
