package com.kite.beauty.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.auth.model.LoginUserContext;
import com.kite.beauty.dto.request.AppointmentRequests;
import com.kite.beauty.dto.response.BeautyAppointmentResponse;
import com.kite.beauty.entity.BeautyAppointment;
import com.kite.beauty.entity.BeautyAppointmentLog;
import com.kite.beauty.entity.BeautyMember;
import com.kite.beauty.entity.BeautyStore;
import com.kite.beauty.mapper.BeautyAppointmentLogMapper;
import com.kite.beauty.mapper.BeautyAppointmentMapper;
import com.kite.beauty.mapper.BeautyMemberMapper;
import com.kite.beauty.mapper.BeautyStoreMapper;
import com.kite.beauty.support.BeautyTenantSupport;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysEmployee;
import com.kite.user.mapper.SysEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeautyAppointmentService extends ServiceImpl<BeautyAppointmentMapper, BeautyAppointment> {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CONFIRMED = 1;
    public static final int STATUS_ARRIVED = 2;
    public static final int STATUS_SERVING = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_CANCELED = 5;
    public static final int STATUS_NO_SHOW = 6;

    private static final DateTimeFormatter APPOINTMENT_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<Integer> EDITABLE_STATUSES = Set.of(STATUS_PENDING, STATUS_CONFIRMED);
    private static final Set<Integer> TERMINAL_STATUSES = Set.of(STATUS_COMPLETED, STATUS_CANCELED, STATUS_NO_SHOW);

    private final BeautyAppointmentLogMapper appointmentLogMapper;
    private final BeautyMemberMapper memberMapper;
    private final BeautyStoreMapper storeMapper;
    private final SysEmployeeMapper employeeMapper;

    public PageResult<BeautyAppointmentResponse> pageAppointments(int pageNum, int pageSize,
                                                                  String keyword, Long memberId,
                                                                  Long storeId, Long employeeId,
                                                                  Integer status,
                                                                  LocalDateTime startFrom,
                                                                  LocalDateTime startTo) {
        IPage<BeautyAppointment> page = baseMapper.selectPageWithDetail(new Page<>(pageNum, pageSize),
                normalizeKeyword(keyword), memberId, storeId, employeeId, status, startFrom, startTo);
        List<BeautyAppointmentResponse> records = page.getRecords().stream()
                .map(BeautyAppointmentResponse::from)
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<BeautyAppointmentResponse> calendarAppointments(Long storeId, Long employeeId,
                                                                LocalDateTime startFrom, LocalDateTime startTo) {
        return baseMapper.selectCalendarWithDetail(storeId, employeeId, startFrom, startTo).stream()
                .map(BeautyAppointmentResponse::from)
                .collect(Collectors.toList());
    }

    public BeautyAppointmentResponse getAppointmentDetail(Long id) {
        return BeautyAppointmentResponse.from(requireAppointmentDetail(id));
    }

    @Transactional
    public void addAppointment(AppointmentRequests.Save request) {
        validateAppointmentPayload(request.getMemberId(), request.getStoreId(), request.getEmployeeId(),
                request.getStartTime(), request.getEndTime(), null);
        BeautyAppointment appointment = toEntity(request);
        appointment.setTenantId(BeautyTenantSupport.currentTenantId());
        appointment.setAppointmentNo(generateAppointmentNo());
        appointment.setStatus(STATUS_PENDING);
        save(appointment);
        insertLog(appointment, "CREATE", null, STATUS_PENDING, null, null, null);
    }

    @Transactional
    public void updateAppointment(Long id, AppointmentRequests.Update request) {
        BeautyAppointment exist = requireAppointment(id);
        assertEditable(exist);
        validateAppointmentPayload(request.getMemberId(), request.getStoreId(), request.getEmployeeId(),
                request.getStartTime(), request.getEndTime(), id);

        BeautyAppointment update = toEntity(request);
        lambdaUpdate()
                .eq(BeautyAppointment::getId, id)
                .set(BeautyAppointment::getMemberId, update.getMemberId())
                .set(BeautyAppointment::getStoreId, update.getStoreId())
                .set(BeautyAppointment::getServiceItemId, update.getServiceItemId())
                .set(BeautyAppointment::getServiceItemName, update.getServiceItemName())
                .set(BeautyAppointment::getEmployeeId, update.getEmployeeId())
                .set(BeautyAppointment::getStartTime, update.getStartTime())
                .set(BeautyAppointment::getEndTime, update.getEndTime())
                .set(BeautyAppointment::getSource, update.getSource())
                .set(BeautyAppointment::getRemark, update.getRemark())
                .update();

        BeautyAppointment after = requireAppointment(id);
        insertLog(after, "UPDATE", exist.getStatus(), exist.getStatus(), null, exist, after);
    }

    @Transactional
    public void reschedule(Long id, AppointmentRequests.Reschedule request) {
        BeautyAppointment exist = requireAppointment(id);
        assertEditable(exist);
        validateAppointmentPayload(exist.getMemberId(), exist.getStoreId(), exist.getEmployeeId(),
                request.getStartTime(), request.getEndTime(), id);

        BeautyAppointment update = new BeautyAppointment();
        update.setId(id);
        update.setStartTime(request.getStartTime());
        update.setEndTime(request.getEndTime());
        updateById(update);

        BeautyAppointment after = requireAppointment(id);
        insertLog(after, "RESCHEDULE", exist.getStatus(), exist.getStatus(), trimToNull(request.getReason()), exist, after);
    }

    @Transactional
    public void confirm(Long id) {
        changeStatus(id, STATUS_PENDING, STATUS_CONFIRMED, "CONFIRM", null);
    }

    @Transactional
    public void arrive(Long id) {
        changeStatus(id, STATUS_CONFIRMED, STATUS_ARRIVED, "ARRIVE", null);
    }

    @Transactional
    public void start(Long id) {
        changeStatus(id, STATUS_ARRIVED, STATUS_SERVING, "START", null);
    }

    @Transactional
    public void complete(Long id) {
        changeStatus(id, STATUS_SERVING, STATUS_COMPLETED, "COMPLETE", null);
    }

    @Transactional
    public void cancel(Long id, String reason) {
        BeautyAppointment appointment = requireAppointment(id);
        if (!Set.of(STATUS_PENDING, STATUS_CONFIRMED, STATUS_ARRIVED).contains(appointment.getStatus())) {
            throw new BusinessException("当前预约状态不能取消");
        }
        BeautyAppointment update = new BeautyAppointment();
        update.setId(id);
        update.setStatus(STATUS_CANCELED);
        update.setCancelReason(trimToNull(reason));
        updateById(update);
        insertLog(requireAppointment(id), "CANCEL", appointment.getStatus(), STATUS_CANCELED, trimToNull(reason), appointment, null);
    }

    @Transactional
    public void noShow(Long id, String reason) {
        BeautyAppointment appointment = requireAppointment(id);
        if (!Integer.valueOf(STATUS_CONFIRMED).equals(appointment.getStatus())) {
            throw new BusinessException("只有已确认预约可以标记爽约");
        }
        BeautyAppointment update = new BeautyAppointment();
        update.setId(id);
        update.setStatus(STATUS_NO_SHOW);
        update.setNoShowReason(trimToNull(reason));
        updateById(update);
        insertLog(requireAppointment(id), "NO_SHOW", appointment.getStatus(), STATUS_NO_SHOW, trimToNull(reason), appointment, null);
    }

    List<BeautyAppointmentResponse> listMemberAppointments(Long memberId, int limit) {
        IPage<BeautyAppointment> page = baseMapper.selectPageWithDetail(new Page<>(1, clampLimit(limit)),
                null, memberId, null, null, null, null, null);
        return page.getRecords().stream()
                .map(BeautyAppointmentResponse::from)
                .collect(Collectors.toList());
    }

    private void changeStatus(Long id, int expectedStatus, int targetStatus, String action, String reason) {
        BeautyAppointment appointment = requireAppointment(id);
        if (!Integer.valueOf(expectedStatus).equals(appointment.getStatus())) {
            throw new BusinessException("当前预约状态不能执行该操作");
        }
        BeautyAppointment update = new BeautyAppointment();
        update.setId(id);
        update.setStatus(targetStatus);
        updateById(update);
        insertLog(requireAppointment(id), action, appointment.getStatus(), targetStatus, reason, appointment, null);
    }

    private BeautyAppointment requireAppointment(Long id) {
        BeautyAppointment appointment = getById(id);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        return appointment;
    }

    private BeautyAppointment requireAppointmentDetail(Long id) {
        BeautyAppointment appointment = baseMapper.selectDetailById(id);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        return appointment;
    }

    private void assertEditable(BeautyAppointment appointment) {
        if (appointment.getStatus() == null || TERMINAL_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessException("终态预约不能编辑");
        }
        if (!EDITABLE_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessException("当前预约状态不能编辑");
        }
    }

    private void validateAppointmentPayload(Long memberId, Long storeId, Long employeeId,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            Long excludeAppointmentId) {
        validateTimeRange(startTime, endTime);
        validateStoreAvailable(storeId);
        validateMemberAvailable(memberId);
        validateEmployeeAvailable(employeeId, storeId);
        validateEmployeeTimeConflict(employeeId, startTime, endTime, excludeAppointmentId);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("预约开始和结束时间不能为空");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("预约结束时间必须晚于开始时间");
        }
    }

    private void validateStoreAvailable(Long storeId) {
        BeautyStore store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException("预约门店不存在");
        }
        if (!Integer.valueOf(1).equals(store.getStatus())) {
            throw new BusinessException("当前门店不在营业中，不能创建或改期预约");
        }
    }

    private void validateMemberAvailable(Long memberId) {
        BeautyMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("预约会员不存在");
        }
        if (!Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException("禁用会员不能创建预约");
        }
    }

    private void validateEmployeeAvailable(Long employeeId, Long storeId) {
        if (employeeId == null) {
            return;
        }
        SysEmployee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("服务员工不存在");
        }
        if (!storeId.equals(employee.getStoreId())) {
            throw new BusinessException("服务员工不属于预约门店");
        }
        Integer status = employee.getStatus();
        if (!Integer.valueOf(1).equals(status) && !Integer.valueOf(2).equals(status)) {
            throw new BusinessException("离职员工不能分配预约");
        }
        if (!Integer.valueOf(1).equals(employee.getServiceEnabled())) {
            throw new BusinessException("该员工未开启服务能力");
        }
    }

    private void validateEmployeeTimeConflict(Long employeeId, LocalDateTime startTime,
                                              LocalDateTime endTime, Long excludeAppointmentId) {
        if (employeeId == null) {
            return;
        }
        Long conflictCount = baseMapper.countEmployeeTimeConflict(employeeId, startTime, endTime, excludeAppointmentId);
        if (conflictCount != null && conflictCount > 0) {
            throw new BusinessException("服务员工在该时间段已有预约");
        }
    }

    private BeautyAppointment toEntity(AppointmentRequests.Save request) {
        BeautyAppointment appointment = new BeautyAppointment();
        appointment.setMemberId(request.getMemberId());
        appointment.setStoreId(request.getStoreId());
        appointment.setServiceItemId(request.getServiceItemId());
        appointment.setServiceItemName(trim(request.getServiceItemName()));
        appointment.setEmployeeId(request.getEmployeeId());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setSource(trimToNull(request.getSource()));
        appointment.setRemark(trimToNull(request.getRemark()));
        return appointment;
    }

    private void insertLog(BeautyAppointment appointment, String action,
                           Integer fromStatus, Integer toStatus, String reason,
                           BeautyAppointment oldAppointment, BeautyAppointment newAppointment) {
        BeautyAppointmentLog log = new BeautyAppointmentLog();
        log.setTenantId(appointment.getTenantId() != null ? appointment.getTenantId() : BeautyTenantSupport.currentTenantId());
        log.setAppointmentId(appointment.getId());
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(LoginUserContext.getUserId());
        log.setReason(trimToNull(reason));
        if (oldAppointment != null) {
            log.setOldStartTime(oldAppointment.getStartTime());
            log.setOldEndTime(oldAppointment.getEndTime());
        }
        if (newAppointment != null) {
            log.setNewStartTime(newAppointment.getStartTime());
            log.setNewEndTime(newAppointment.getEndTime());
        }
        appointmentLogMapper.insert(log);
    }

    private String generateAppointmentNo() {
        for (int i = 0; i < 5; i++) {
            String appointmentNo = "A" + LocalDateTime.now().format(APPOINTMENT_NO_TIME_FORMATTER)
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
            Long count = baseMapper.selectCount(new LambdaQueryWrapper<BeautyAppointment>()
                    .eq(BeautyAppointment::getAppointmentNo, appointmentNo));
            if (count == 0) {
                return appointmentNo;
            }
        }
        throw new BusinessException("预约编号生成失败，请重试");
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
