package com.kite.beauty.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

public final class StoreRequests {

    private StoreRequests() {
    }

    @Data
    public static class Save {
        @NotBlank(message = "门店编码不能为空")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{1,31}$", message = "门店编码需以字母开头，只能包含字母、数字、下划线和中划线，长度2-32位")
        private String storeCode;

        @NotBlank(message = "门店名称不能为空")
        @Size(max = 100, message = "门店名称长度不能超过100个字符")
        private String storeName;

        @Min(value = 1, message = "负责人ID无效")
        private Long managerEmployeeId;

        @Size(max = 20, message = "联系电话长度不能超过20个字符")
        private String phone;

        @Size(max = 64, message = "省份长度不能超过64个字符")
        private String province;

        @Size(max = 64, message = "城市长度不能超过64个字符")
        private String city;

        @Size(max = 64, message = "区县长度不能超过64个字符")
        private String district;

        @Size(max = 255, message = "详细地址长度不能超过255个字符")
        private String address;

        private Map<String, Object> businessHours;

        @NotNull(message = "营业状态不能为空")
        @Min(value = 0, message = "营业状态值无效")
        @Max(value = 2, message = "营业状态值无效")
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
        @NotNull(message = "营业状态不能为空")
        @Min(value = 0, message = "营业状态值无效")
        @Max(value = 2, message = "营业状态值无效")
        private Integer status;
    }
}
