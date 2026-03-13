package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;

/**
 * 角色实体
 */
@TableName("sys_role")
public class SysRole extends BaseEntity {
    
    private Long tenantId;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer sortOrder;
    private Integer status;
    
    /**
     * 数据权限范围: 1全部 2本部门 3本部门及下级 4仅本人 5自定义部门
     */
    private Integer dataScope;
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
}
