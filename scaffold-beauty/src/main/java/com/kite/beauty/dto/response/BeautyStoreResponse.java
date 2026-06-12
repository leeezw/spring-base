package com.kite.beauty.dto.response;

import com.kite.beauty.entity.BeautyStore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class BeautyStoreResponse {

    private Long id;
    private Long tenantId;
    private String storeCode;
    private String storeName;
    private Long managerEmployeeId;
    private String managerEmployeeName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String address;
    private Map<String, Object> businessHours;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BeautyStoreResponse from(BeautyStore store) {
        if (store == null) {
            return null;
        }

        BeautyStoreResponse response = new BeautyStoreResponse();
        response.setId(store.getId());
        response.setTenantId(store.getTenantId());
        response.setStoreCode(store.getStoreCode());
        response.setStoreName(store.getStoreName());
        response.setManagerEmployeeId(store.getManagerEmployeeId());
        response.setPhone(store.getPhone());
        response.setProvince(store.getProvince());
        response.setCity(store.getCity());
        response.setDistrict(store.getDistrict());
        response.setAddress(store.getAddress());
        response.setBusinessHours(store.getBusinessHours());
        response.setStatus(store.getStatus());
        response.setRemark(store.getRemark());
        response.setCreateTime(store.getCreateTime());
        response.setUpdateTime(store.getUpdateTime());
        return response;
    }
}
