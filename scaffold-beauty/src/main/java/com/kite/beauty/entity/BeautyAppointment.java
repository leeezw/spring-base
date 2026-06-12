package com.kite.beauty.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 美业预约
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("beauty_appointment")
public class BeautyAppointment extends BaseEntity {

    private Long tenantId;
    private String appointmentNo;
    private Long memberId;
    private Long storeId;
    private Long serviceItemId;
    private String serviceItemName;
    private Long employeeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /**
     * 0待确认 1已确认 2已到店 3服务中 4已完成 5已取消 6爽约
     */
    private Integer status;
    private String source;
    private String cancelReason;
    private String noShowReason;
    private String remark;

    @TableField(exist = false)
    private String memberName;

    @TableField(exist = false)
    private String memberPhone;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String employeeName;
}
