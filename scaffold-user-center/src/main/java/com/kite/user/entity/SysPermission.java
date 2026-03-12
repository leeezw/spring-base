package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;

/**
 * 权限实体
 */
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    
    private Long tenantId;
    private String permissionCode;
    private String permissionName;
    private Integer permissionType;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
    
    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
    
    public Integer getPermissionType() { return permissionType; }
    public void setPermissionType(Integer permissionType) { this.permissionType = permissionType; }
    
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
