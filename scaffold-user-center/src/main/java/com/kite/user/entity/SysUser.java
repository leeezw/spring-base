package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;

import java.util.List;

/**
 * 用户实体
 */
@TableName("sys_user")
public class SysUser extends BaseEntity {
    
    private Long tenantId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Long deptId;
    private Integer status;
    
    /** 部门名称（非数据库字段） */
    @TableField(exist = false)
    private String deptName;
    
    /** 角色列表（非数据库字段） */
    @TableField(exist = false)
    private List<RoleInfo> roles;
    
    /** 岗位列表（非数据库字段） */
    @TableField(exist = false)
    private List<PostInfo> posts;
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    
    public List<RoleInfo> getRoles() { return roles; }
    public void setRoles(List<RoleInfo> roles) { this.roles = roles; }
    
    public List<PostInfo> getPosts() { return posts; }
    public void setPosts(List<PostInfo> posts) { this.posts = posts; }
    
    /**
     * 角色简要信息
     */
    public static class RoleInfo {
        private Long id;
        private String roleCode;
        private String roleName;
        
        public RoleInfo() {}
        public RoleInfo(Long id, String roleCode, String roleName) {
            this.id = id; this.roleCode = roleCode; this.roleName = roleName;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
    }

    /**
     * 岗位简要信息
     */
    public static class PostInfo {
        private Long id;
        private String postCode;
        private String postName;

        public PostInfo() {}
        public PostInfo(Long id, String postCode, String postName) {
            this.id = id; this.postCode = postCode; this.postName = postName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPostCode() { return postCode; }
        public void setPostCode(String postCode) { this.postCode = postCode; }
        public String getPostName() { return postName; }
        public void setPostName(String postName) { this.postName = postName; }
    }
}
