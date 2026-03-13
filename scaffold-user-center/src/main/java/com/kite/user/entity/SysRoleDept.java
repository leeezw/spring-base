package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色-部门关联（数据权限-自定义部门）
 */
@TableName("sys_role_dept")
public class SysRoleDept {
    private Long roleId;
    private Long deptId;
    
    public SysRoleDept() {}
    
    public SysRoleDept(Long roleId, Long deptId) {
        this.roleId = roleId;
        this.deptId = deptId;
    }
    
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
}
