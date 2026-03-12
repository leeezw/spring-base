package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;

import java.util.List;

/**
 * 部门实体
 */
@TableName("sys_dept")
public class SysDept extends BaseEntity {
    
    private Long tenantId;
    private String deptName;
    private Long parentId;
    private String leader;
    private String phone;
    private String email;
    private Integer sortOrder;
    private Integer status;
    
    @TableField(exist = false)
    private List<SysDept> children;
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public List<SysDept> getChildren() { return children; }
    public void setChildren(List<SysDept> children) { this.children = children; }
}
