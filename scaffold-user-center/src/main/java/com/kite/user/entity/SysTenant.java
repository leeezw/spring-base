package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 租户实体
 */
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {
    
    private String tenantCode;
    private String tenantName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private LocalDateTime expireTime;
    private Integer accountCount;
    private Integer status;
    private String logo;

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    
    public Integer getAccountCount() { return accountCount; }
    public void setAccountCount(Integer accountCount) { this.accountCount = accountCount; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
}
