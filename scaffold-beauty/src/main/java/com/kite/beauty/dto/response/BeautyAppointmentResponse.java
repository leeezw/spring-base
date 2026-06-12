package com.kite.beauty.dto.response;

import com.kite.beauty.entity.BeautyAppointment;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BeautyAppointmentResponse {

    private Long id;
    private Long tenantId;
    private String appointmentNo;
    private Long memberId;
    private String memberName;
    private String memberPhone;
    private Long storeId;
    private String storeName;
    private Long serviceItemId;
    private String serviceItemName;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String source;
    private String cancelReason;
    private String noShowReason;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BeautyAppointmentResponse from(BeautyAppointment appointment) {
        if (appointment == null) {
            return null;
        }
        BeautyAppointmentResponse response = new BeautyAppointmentResponse();
        response.setId(appointment.getId());
        response.setTenantId(appointment.getTenantId());
        response.setAppointmentNo(appointment.getAppointmentNo());
        response.setMemberId(appointment.getMemberId());
        response.setMemberName(appointment.getMemberName());
        response.setMemberPhone(appointment.getMemberPhone());
        response.setStoreId(appointment.getStoreId());
        response.setStoreName(appointment.getStoreName());
        response.setServiceItemId(appointment.getServiceItemId());
        response.setServiceItemName(appointment.getServiceItemName());
        response.setEmployeeId(appointment.getEmployeeId());
        response.setEmployeeName(appointment.getEmployeeName());
        response.setStartTime(appointment.getStartTime());
        response.setEndTime(appointment.getEndTime());
        response.setStatus(appointment.getStatus());
        response.setSource(appointment.getSource());
        response.setCancelReason(appointment.getCancelReason());
        response.setNoShowReason(appointment.getNoShowReason());
        response.setRemark(appointment.getRemark());
        response.setCreateTime(appointment.getCreateTime());
        response.setUpdateTime(appointment.getUpdateTime());
        return response;
    }
}
