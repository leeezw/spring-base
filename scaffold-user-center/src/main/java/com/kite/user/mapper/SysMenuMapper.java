package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kite.user.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    
    /**
     * 根据用户ID查询菜单ID列表
     */
    @Select("SELECT DISTINCT m.id " +
            "FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.deleted = 0 " +
            "AND m.status = 1 " +
            "AND m.visible = 1")
    List<Long> selectMenuIdsByUserId(Long userId);

    /**
     * 查询所有有效菜单ID
     */
    @Select("SELECT id FROM sys_menu WHERE deleted = 0 AND status = 1")
    List<Long> selectAllMenuIds();
}
