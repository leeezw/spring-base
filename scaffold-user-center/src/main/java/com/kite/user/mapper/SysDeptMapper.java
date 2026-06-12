package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.user.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 部门Mapper
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    @Select("""
            <script>
            SELECT d.*,
                   s.store_name
            FROM sys_dept d
                     LEFT JOIN beauty_store s ON d.store_id = s.id AND s.deleted = 0
            WHERE d.deleted = 0
            <if test="deptName != null and deptName != ''">
              AND d.dept_name LIKE CONCAT('%', #{deptName}, '%')
            </if>
            <if test="storeId != null">
              AND d.store_id = #{storeId}
            </if>
            <if test="status != null">
              AND d.status = #{status}
            </if>
            ORDER BY d.sort_order ASC, d.id ASC
            </script>
            """)
    IPage<SysDept> selectPageWithStore(Page<SysDept> page,
                                       @Param("deptName") String deptName,
                                       @Param("storeId") Long storeId,
                                       @Param("status") Integer status);

    @Select("""
            <script>
            SELECT d.*,
                   s.store_name
            FROM sys_dept d
                     LEFT JOIN beauty_store s ON d.store_id = s.id AND s.deleted = 0
            WHERE d.deleted = 0
            <if test="storeId != null">
              AND d.store_id = #{storeId}
            </if>
            ORDER BY d.sort_order ASC, d.id ASC
            </script>
            """)
    List<SysDept> selectTreeWithStore(@Param("storeId") Long storeId);

    @Select("""
            SELECT d.*,
                   s.store_name
            FROM sys_dept d
                     LEFT JOIN beauty_store s ON d.store_id = s.id AND s.deleted = 0
            WHERE d.id = #{id}
              AND d.deleted = 0
            """)
    SysDept selectDetailById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM beauty_store
            WHERE id = #{storeId}
              AND deleted = 0
            """)
    Long countStoreById(@Param("storeId") Long storeId);

    @Select("""
            SELECT id,
                   store_code AS "storeCode",
                   store_name AS "storeName",
                   status
            FROM beauty_store
            WHERE deleted = 0
            ORDER BY store_code ASC, id ASC
            """)
    List<Map<String, Object>> selectStoreOptions();
}
