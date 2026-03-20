package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.user.entity.SysEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 员工 Mapper
 */
@Mapper
public interface SysEmployeeMapper extends BaseMapper<SysEmployee> {

    /**
     * 联表分页查询：自动填充 deptName / postName / username
     * 多租户过滤由 TenantLineInnerInterceptor 自动注入到主表 e 上
     */
    @Select("""
            SELECT e.*,
                   d.dept_name,
                   p.post_name,
                   u.username
            FROM sys_employee e
                     LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.deleted = 0
                     LEFT JOIN sys_post p ON e.post_id = p.id AND p.deleted = 0
                     LEFT JOIN sys_user u ON e.user_id = u.id AND u.deleted = 0
            ${ew.customSqlSegment}
            """)
    IPage<SysEmployee> selectPageWithDetail(Page<SysEmployee> page,
                                            @Param(Constants.WRAPPER) com.baomidou.mybatisplus.core.conditions.Wrapper<SysEmployee> wrapper);

    /**
     * 按 ID 查询详情（含关联名称）
     */
    @Select("""
            SELECT e.*,
                   d.dept_name,
                   p.post_name,
                   u.username
            FROM sys_employee e
                     LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.deleted = 0
                     LEFT JOIN sys_post p ON e.post_id = p.id AND p.deleted = 0
                     LEFT JOIN sys_user u ON e.user_id = u.id AND u.deleted = 0
            WHERE e.id = #{id} AND e.deleted = 0
            """)
    SysEmployee selectDetailById(@Param("id") Long id);
}
