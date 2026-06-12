package com.kite.beauty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.beauty.entity.BeautyAppointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BeautyAppointmentMapper extends BaseMapper<BeautyAppointment> {

    @Select("""
            <script>
            SELECT a.*,
                   m.name AS member_name,
                   m.phone AS member_phone,
                   s.store_name,
                   e.emp_name AS employee_name
            FROM beauty_appointment a
                     LEFT JOIN beauty_member m ON a.member_id = m.id AND m.deleted = 0
                     LEFT JOIN beauty_store s ON a.store_id = s.id AND s.deleted = 0
                     LEFT JOIN sys_employee e ON a.employee_id = e.id AND e.deleted = 0
            WHERE a.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (a.appointment_no LIKE CONCAT('%', #{keyword}, '%')
                   OR a.service_item_name LIKE CONCAT('%', #{keyword}, '%')
                   OR m.name LIKE CONCAT('%', #{keyword}, '%')
                   OR m.phone LIKE CONCAT('%', #{keyword}, '%')
                   OR e.emp_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="memberId != null">
              AND a.member_id = #{memberId}
            </if>
            <if test="storeId != null">
              AND a.store_id = #{storeId}
            </if>
            <if test="employeeId != null">
              AND a.employee_id = #{employeeId}
            </if>
            <if test="status != null">
              AND a.status = #{status}
            </if>
            <if test="startFrom != null">
              AND a.start_time &gt;= #{startFrom}
            </if>
            <if test="startTo != null">
              AND a.start_time &lt;= #{startTo}
            </if>
            ORDER BY a.start_time DESC, a.id DESC
            </script>
            """)
    IPage<BeautyAppointment> selectPageWithDetail(Page<BeautyAppointment> page,
                                                  @Param("keyword") String keyword,
                                                  @Param("memberId") Long memberId,
                                                  @Param("storeId") Long storeId,
                                                  @Param("employeeId") Long employeeId,
                                                  @Param("status") Integer status,
                                                  @Param("startFrom") LocalDateTime startFrom,
                                                  @Param("startTo") LocalDateTime startTo);

    @Select("""
            <script>
            SELECT a.*,
                   m.name AS member_name,
                   m.phone AS member_phone,
                   s.store_name,
                   e.emp_name AS employee_name
            FROM beauty_appointment a
                     LEFT JOIN beauty_member m ON a.member_id = m.id AND m.deleted = 0
                     LEFT JOIN beauty_store s ON a.store_id = s.id AND s.deleted = 0
                     LEFT JOIN sys_employee e ON a.employee_id = e.id AND e.deleted = 0
            WHERE a.deleted = 0
            <if test="storeId != null">
              AND a.store_id = #{storeId}
            </if>
            <if test="employeeId != null">
              AND a.employee_id = #{employeeId}
            </if>
            <if test="startFrom != null">
              AND a.start_time &gt;= #{startFrom}
            </if>
            <if test="startTo != null">
              AND a.start_time &lt;= #{startTo}
            </if>
            ORDER BY a.start_time ASC, a.id ASC
            </script>
            """)
    List<BeautyAppointment> selectCalendarWithDetail(@Param("storeId") Long storeId,
                                                     @Param("employeeId") Long employeeId,
                                                     @Param("startFrom") LocalDateTime startFrom,
                                                     @Param("startTo") LocalDateTime startTo);

    @Select("""
            SELECT a.*,
                   m.name AS member_name,
                   m.phone AS member_phone,
                   s.store_name,
                   e.emp_name AS employee_name
            FROM beauty_appointment a
                     LEFT JOIN beauty_member m ON a.member_id = m.id AND m.deleted = 0
                     LEFT JOIN beauty_store s ON a.store_id = s.id AND s.deleted = 0
                     LEFT JOIN sys_employee e ON a.employee_id = e.id AND e.deleted = 0
            WHERE a.id = #{id}
              AND a.deleted = 0
            """)
    BeautyAppointment selectDetailById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM beauty_appointment
            WHERE deleted = 0
              AND employee_id = #{employeeId}
              AND status NOT IN (4, 5, 6)
              AND start_time < #{endTime}
              AND end_time > #{startTime}
              AND (#{excludeId} IS NULL OR id <> #{excludeId})
            """)
    Long countEmployeeTimeConflict(@Param("employeeId") Long employeeId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("excludeId") Long excludeId);
}
