package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 员工自定义字段定义
 * 每个租户可配置专属的员工扩展字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_employee_field", autoResultMap = true)
public class SysEmployeeField extends BaseEntity {

    private Long tenantId;

    /**
     * 字段 key，存入 custom_fields JSON 时作为键，租户内唯一
     * 示例：blood_type
     */
    private String fieldKey;

    /**
     * 显示标签
     * 示例：血型
     */
    private String fieldLabel;

    /**
     * 字段类型：text / number / date / select
     */
    private String fieldType;

    /**
     * 下拉选项（fieldType=select 时有效）
     * 示例：["A", "B", "AB", "O"]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    /**
     * 是否必填：1必填 0选填
     */
    private Integer required;

    /**
     * 排序
     */
    private Integer sortOrder;
}
