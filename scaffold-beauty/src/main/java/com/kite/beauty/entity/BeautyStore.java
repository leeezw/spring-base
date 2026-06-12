package com.kite.beauty.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 美业门店
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "beauty_store", autoResultMap = true)
public class BeautyStore extends BaseEntity {

    private Long tenantId;

    private String storeCode;

    private String storeName;

    private Long managerEmployeeId;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String address;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> businessHours;

    /**
     * 1营业中 0停业 2装修中
     */
    private Integer status;

    private String remark;
}
