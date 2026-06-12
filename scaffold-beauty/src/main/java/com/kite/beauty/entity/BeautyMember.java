package com.kite.beauty.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 美业会员档案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "beauty_member", autoResultMap = true)
public class BeautyMember extends BaseEntity {

    private Long tenantId;

    private String memberNo;

    private String name;

    private String phone;

    /**
     * 0未知 1男 2女
     */
    private Integer gender;

    private LocalDate birthday;

    private String source;

    private String level;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private Long belongStoreId;

    private BigDecimal totalConsumeAmount;

    private LocalDateTime lastConsumeTime;

    /**
     * 1正常 0禁用
     */
    private Integer status;

    private String remark;
}
