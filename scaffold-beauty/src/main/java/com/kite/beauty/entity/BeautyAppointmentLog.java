package com.kite.beauty.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 美业预约操作日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("beauty_appointment_log")
public class BeautyAppointmentLog extends BaseEntity {

    private Long tenantId;
    private Long appointmentId;
    private String action;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorId;
    private String reason;
    private LocalDateTime oldStartTime;
    private LocalDateTime oldEndTime;
    private LocalDateTime newStartTime;
    private LocalDateTime newEndTime;
    private String remark;
}
