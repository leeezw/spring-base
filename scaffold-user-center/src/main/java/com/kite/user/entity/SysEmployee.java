package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Map;

/**
 * 员工档案
 * 员工与系统账号解耦，userId 可空，按需开通账号
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_employee", autoResultMap = true)
public class SysEmployee extends BaseEntity {

    private Long tenantId;

    // ── 关联 ──────────────────────────────────────────────────

    /**
     * 关联系统账号 ID（可空，未开通账号时为 null）
     */
    private Long userId;

    /**
     * 所属部门 ID
     */
    private Long deptId;

    /**
     * 主岗位 ID（可空）
     */
    private Long postId;

    /**
     * 职位 ID（可空）
     */
    private Long positionId;

    // ── 基础档案 ──────────────────────────────────────────────

    /**
     * 工号（租户内唯一）
     */
    private String empCode;

    /**
     * 员工姓名
     */
    private String empName;

    /**
     * 性别：0未知 1男 2女
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像 URL
     */
    private String avatar;

    // ── 身份信息 ──────────────────────────────────────────────

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 出生日期
     */
    private LocalDate birthday;

    /**
     * 民族
     */
    private String nation;

    /**
     * 籍贯
     */
    private String nativePlace;

    /**
     * 现住址
     */
    private String address;

    // ── 入职信息 ──────────────────────────────────────────────

    /**
     * 入职日期
     */
    private LocalDate hireDate;

    /**
     * 员工类型：1正式 2试用 3实习
     */
    private Integer empType;

    /**
     * 试用期结束日期
     */
    private LocalDate probationEndDate;

    /**
     * 在职状态：1在职 2试用中 0离职
     */
    private Integer status;

    // ── 紧急联系人 ────────────────────────────────────────────

    /**
     * 紧急联系人姓名
     */
    private String emergencyContact;

    /**
     * 紧急联系人电话
     */
    private String emergencyPhone;

    /**
     * 与本人关系
     */
    private String emergencyRelation;

    // ── 自定义字段 ────────────────────────────────────────────

    /**
     * 自定义字段值（JSONB）
     * key = SysEmployeeField.fieldKey，value = 用户录入值
     * 示例：{"blood_type": "A", "hobby": "游泳"}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> customFields;

    // ── 非 DB 关联字段 ────────────────────────────────────────

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String postName;

    @TableField(exist = false)
    private String positionName;

    /**
     * 绑定账号的用户名（来自 SysUser）
     */
    @TableField(exist = false)
    private String username;
}
