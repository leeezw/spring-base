package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kite.user.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    
    /**
     * 查询所有有效的系统内置权限ID（tenant_id=0）
     */
    @Select("SELECT id FROM sys_permission WHERE deleted = 0 AND status = 1 AND tenant_id = 0")
    List<Long> selectBuiltinPermissionIds();
}
