package com.kite.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户信息
 */
@Data
public class LoginUser implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 部门ID
     */
    private Long deptId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像
     */
    private String avatar;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户Logo
     */
    private String tenantLogo;
    
    /**
     * 角色列表
     */
    private List<String> roles;
    
    /**
     * 权限列表
     */
    private List<String> permissions;
    
    /**
     * 登录时间
     */
    private Long loginTime;
    
    /**
     * 过期时间
     */
    private Long expireTime;
    
    /**
     * Token
     */
    private String token;
}
