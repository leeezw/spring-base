package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kite.user.entity.SysRolePermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-权限关联Mapper
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
    
    @Insert("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
