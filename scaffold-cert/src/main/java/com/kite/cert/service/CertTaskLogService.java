package com.kite.cert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.cert.dto.response.TaskLogResponse;
import com.kite.cert.entity.CertTaskLog;
import com.kite.cert.mapper.CertTaskLogMapper;
import com.kite.cert.support.CertTenantSupport;
import com.kite.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务日志服务。
 */
@Service
@RequiredArgsConstructor
public class CertTaskLogService extends ServiceImpl<CertTaskLogMapper, CertTaskLog> {

    public PageResult<TaskLogResponse> page(int pageNum, int pageSize, Long certificateId, String type, String status) {
        LambdaQueryWrapper<CertTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(certificateId != null, CertTaskLog::getCertificateId, certificateId)
                .eq(type != null, CertTaskLog::getType, type)
                .eq(status != null, CertTaskLog::getStatus, status)
                .orderByDesc(CertTaskLog::getId);
        IPage<CertTaskLog> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<TaskLogResponse> records = page.getRecords().stream()
                .map(TaskLogResponse::from).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 创建一条 RUNNING 状态的任务日志并返回其 id。 */
    public CertTaskLog start(Long certificateId, String type, String triggerSource) {
        CertTaskLog log = new CertTaskLog();
        log.setTenantId(CertTenantSupport.currentTenantId());
        log.setCertificateId(certificateId);
        log.setType(type);
        log.setTriggerSource(triggerSource);
        log.setStatus("RUNNING");
        log.setStartedAt(LocalDateTime.now());
        save(log);
        return log;
    }

    public void finish(CertTaskLog log, boolean success, String message, String detail) {
        log.setStatus(success ? "SUCCESS" : "FAILED");
        log.setMessage(truncate(message, 1000));
        log.setDetail(detail);
        log.setFinishedAt(LocalDateTime.now());
        updateById(log);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
