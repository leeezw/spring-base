package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 多租户过滤由 TenantLineInnerInterceptor 自动注入
     */
    @Select("""
            <script>
            SELECT e.*,
                   d.dept_name,
                   p.post_name,
                   pos.position_name,
                   s.store_name,
                   u.username
            FROM sys_employee e
                     LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.deleted = 0
                     LEFT JOIN sys_post p ON e.post_id = p.id AND p.deleted = 0
                     LEFT JOIN sys_position pos ON e.position_id = pos.id AND pos.deleted = 0
                     LEFT JOIN beauty_store s ON e.store_id = s.id AND s.deleted = 0
                     LEFT JOIN sys_user u ON e.user_id = u.id AND u.deleted = 0
            WHERE e.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (e.emp_name LIKE CONCAT('%', #{keyword}, '%')
                   OR e.emp_code LIKE CONCAT('%', #{keyword}, '%')
                   OR e.phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="deptId != null">
              AND e.dept_id = #{deptId}
            </if>
            <if test="status != null">
              AND e.status = #{status}
            </if>
            <if test="empType != null">
              AND e.emp_type = #{empType}
            </if>
            <if test="storeId != null">
              AND e.store_id = #{storeId}
            </if>
            <if test="serviceEnabled != null">
              AND e.service_enabled = #{serviceEnabled}
            </if>
            ORDER BY e.emp_code ASC
            </script>
            """)
    IPage<SysEmployee> selectPageWithDetail(Page<SysEmployee> page,
                                            @Param("keyword") String keyword,
                                            @Param("deptId") Long deptId,
                                            @Param("status") Integer status,
                                            @Param("empType") Integer empType,
                                            @Param("storeId") Long storeId,
                                            @Param("serviceEnabled") Integer serviceEnabled);

    /**
     * 按 ID 查询详情（含关联名称）
     */
    @Select("""
            SELECT e.*,
                   d.dept_name,
                   p.post_name,
                   pos.position_name,
                   s.store_name,
                   u.username
            FROM sys_employee e
                     LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.deleted = 0
                     LEFT JOIN sys_post p ON e.post_id = p.id AND p.deleted = 0
                     LEFT JOIN sys_position pos ON e.position_id = pos.id AND pos.deleted = 0
                     LEFT JOIN beauty_store s ON e.store_id = s.id AND s.deleted = 0
                     LEFT JOIN sys_user u ON e.user_id = u.id AND u.deleted = 0
            WHERE e.id = #{id} AND e.deleted = 0
            """)
    SysEmployee selectDetailById(@Param("id") Long id);
}
